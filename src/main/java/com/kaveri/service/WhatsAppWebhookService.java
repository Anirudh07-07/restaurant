package com.kaveri.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.config.WhatsAppConfigProperties;
import com.kaveri.entity.Order;
import com.kaveri.entity.User;
import com.kaveri.entity.WhatsAppWebhookEvent;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.OrderRepository;
import com.kaveri.repository.UserRepository;
import com.kaveri.repository.WhatsAppWebhookEventRepository;
import com.kaveri.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private final WhatsAppConfigProperties config;
    private final WhatsAppWebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final WhatsAppNotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final Pattern ACTION_PATTERN = Pattern.compile("^ACTION_([A-Z_]+)_(\\d+)$");
    private static final Pattern TEXT_COMMAND_PATTERN = Pattern.compile("^(ACCEPT|REJECT|CANCEL|PREPARE|COOK|READY|DISPATCH|DELIVER|STATUS)\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Handles Meta Webhook verification handshake (GET).
     */
    public String verifySubscription(String mode, String token, String challenge) {
        if ("subscribe".equals(mode) && config.getWebhook().getVerifyToken().equals(token)) {
            log.info("WhatsApp webhook subscription verified successfully.");
            return challenge;
        }
        log.warn("WhatsApp webhook verification failed. Token mismatch or invalid mode.");
        throw new UnauthorizedException("Webhook verification failed: invalid token or mode");
    }

    /**
     * Validates Meta X-Hub-Signature-256 header.
     */
    public void validateSignature(String payload, String signatureHeader) {
        String appSecret = config.getWebhook().getAppSecret();
        if (appSecret == null || appSecret.isBlank() || config.getMock().isEnabled()) {
            return; // In mock/dev mode without app secret, skip
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new UnauthorizedException("Missing or malformed X-Hub-Signature-256 header");
        }

        String actualHash = signatureHeader.substring(7);
        String expectedHash = new HmacUtils("HmacSHA256", appSecret).hmacHex(payload);

        if (!MessageDigest.isEqual(actualHash.getBytes(), expectedHash.getBytes())) {
            log.error("WhatsApp webhook signature mismatch!");
            throw new UnauthorizedException("Invalid webhook signature");
        }
    }

    /**
     * Processes inbound WhatsApp messages and interactive button responses from makers.
     */
    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        validateSignature(payload, signatureHeader);

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entryArray = root.path("entry");
            if (!entryArray.isArray()) {
                return;
            }

            for (JsonNode entry : entryArray) {
                JsonNode changesArray = entry.path("changes");
                if (!changesArray.isArray()) continue;

                for (JsonNode change : changesArray) {
                    JsonNode value = change.path("value");
                    JsonNode messages = value.path("messages");
                    if (!messages.isArray()) continue;

                    for (JsonNode msg : messages) {
                        processIncomingMessage(msg);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse/process WhatsApp webhook payload: {}", e.getMessage(), e);
        }
    }

    private void processIncomingMessage(JsonNode msg) {
        String messageId = msg.path("id").asText();
        String fromPhone = msg.path("from").asText();

        if (messageId.isBlank() || fromPhone.isBlank()) {
            return;
        }

        // Idempotency: Prevent re-processing duplicate webhook events from Meta
        if (webhookEventRepository.existsByMessageId(messageId)) {
            log.info("Duplicate WhatsApp webhook message {} ignored.", messageId);
            return;
        }

        // Extract action string
        String actionId = null;
        String type = msg.path("type").asText();

        if ("interactive".equals(type)) {
            JsonNode interactive = msg.path("interactive");
            if ("button_reply".equals(interactive.path("type").asText())) {
                actionId = interactive.path("button_reply").path("id").asText();
            }
        } else if ("text".equals(type)) {
            actionId = msg.path("text").path("body").asText().trim();
        }

        if (actionId == null || actionId.isBlank()) {
            log.debug("No recognizable action in message {}", messageId);
            return;
        }

        ParsedAction action = parseAction(actionId);
        if (action == null) {
            log.warn("Unrecognized WhatsApp kitchen action '{}' from {}", actionId, fromPhone);
            return;
        }

        // Load Order
        Order order = orderRepository.findById(action.orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Order #{} referenced in WhatsApp message not found", action.orderId);
            return;
        }

        // Maker Authorization: Ensure sender phone is an authorized kitchen maker or Admin
        if (!isAuthorizedMaker(fromPhone, order)) {
            log.warn("Unauthorized WhatsApp action attempt by phone {} for order #{}", fromPhone, order.getId());
            return;
        }

        // Execute status update through OrderService (same service & rules as web dashboard)
        try {
            orderService.updateOrderStatus(order.getId(), action.targetStatus);

            // Record webhook event for idempotency
            WhatsAppWebhookEvent eventRecord = WhatsAppWebhookEvent.builder()
                    .messageId(messageId)
                    .senderPhone(fromPhone)
                    .actionPayload(actionId)
                    .orderId(order.getId())
                    .statusApplied(action.targetStatus.name())
                    .build();
            webhookEventRepository.save(eventRecord);

            log.info("WhatsApp synchronized Order #{} to status {} by maker {}",
                    order.getId(), action.targetStatus, fromPhone);

            // Send follow-up confirmation and next kitchen steps to maker
            notificationService.notifyMakerStatusFollowup(order, action.targetStatus, fromPhone);

        } catch (BadRequestException e) {
            log.warn("WhatsApp status transition rejected for order #{}: {}", order.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to apply WhatsApp status change to order #{}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private boolean isAuthorizedMaker(String senderPhone, Order order) {
        // 1. Matches configured restaurant phone
        if (PhoneNumberUtils.isMatching(senderPhone, config.getRestaurant().getPhone())) {
            return true;
        }

        // 2. Matches assigned order maker phone
        if (order.getMaker() != null && PhoneNumberUtils.isMatching(senderPhone, order.getMaker().getPhone())) {
            return true;
        }

        // 3. Matches any registered Admin user's phone
        List<User> admins = userRepository.findAll();
        for (User u : admins) {
            if (u.getPhone() != null && PhoneNumberUtils.isMatching(senderPhone, u.getPhone())
                    && u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
                return true;
            }
        }

        return false;
    }

    private ParsedAction parseAction(String text) {
        // 1. Button reply: ACTION_{VERB}_{orderId}
        Matcher buttonMatcher = ACTION_PATTERN.matcher(text.toUpperCase());
        if (buttonMatcher.matches()) {
            String verb = buttonMatcher.group(1);
            long orderId = Long.parseLong(buttonMatcher.group(2));
            OrderStatus status = mapVerbToStatus(verb);
            if (status != null) {
                return new ParsedAction(orderId, status);
            }
        }

        // 2. Text command: ACCEPT 101, COOK 101, READY 101, DELIVER 101
        Matcher textMatcher = TEXT_COMMAND_PATTERN.matcher(text);
        if (textMatcher.matches()) {
            String verb = textMatcher.group(1).toUpperCase();
            long orderId = Long.parseLong(textMatcher.group(2));
            OrderStatus status = mapVerbToStatus(verb);
            if (status != null) {
                return new ParsedAction(orderId, status);
            }
        }

        return null;
    }

    private OrderStatus mapVerbToStatus(String verb) {
        return switch (verb) {
            case "ACCEPT", "CONFIRM" -> OrderStatus.CONFIRMED;
            case "REJECT", "CANCEL" -> OrderStatus.CANCELLED;
            case "PREPARE", "COOK" -> OrderStatus.PREPARING;
            case "READY" -> OrderStatus.READY;
            case "OUT_FOR_DELIVERY", "DISPATCH" -> OrderStatus.OUT_FOR_DELIVERY;
            case "DELIVER", "DELIVERED" -> OrderStatus.DELIVERED;
            default -> null;
        };
    }

    private static class ParsedAction {
        final long orderId;
        final OrderStatus targetStatus;

        ParsedAction(long orderId, OrderStatus targetStatus) {
            this.orderId = orderId;
            this.targetStatus = targetStatus;
        }
    }
}

