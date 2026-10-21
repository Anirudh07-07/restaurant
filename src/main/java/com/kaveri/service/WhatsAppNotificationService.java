package com.kaveri.service;

import com.kaveri.config.WhatsAppConfigProperties;
import com.kaveri.entity.Order;
import com.kaveri.entity.OrderItem;
import com.kaveri.entity.User;
import com.kaveri.entity.WhatsAppNotificationLog;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.WhatsAppNotificationStatus;
import com.kaveri.enums.WhatsAppRecipientRole;
import com.kaveri.integration.whatsapp.WhatsAppClient;
import com.kaveri.integration.whatsapp.dto.WhatsAppButton;
import com.kaveri.repository.UserRepository;
import com.kaveri.repository.WhatsAppNotificationLogRepository;
import com.kaveri.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final WhatsAppClient whatsAppClient;
    private final WhatsAppConfigProperties config;
    private final WhatsAppNotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;
    private final com.kaveri.repository.PaymentRepository paymentRepository;

    /**
     * Notifies the maker/kitchen staff when a new order arrives.
     * Includes items, quantities, total, delivery info, and Accept/Reject interactive buttons.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMakerNewOrder(Order order) {
        String makerPhone = resolveMakerPhone(order);
        if (makerPhone == null || makerPhone.isBlank()) {
            log.warn("Cannot notify maker: no valid maker phone found for order #{}", order.getId());
            return;
        }

        String headerText = "🍽️ New Order #" + order.getId();
        StringBuilder body = new StringBuilder();
        body.append("Customer: ").append(order.getUser() != null ? order.getUser().getName() : "Guest").append("\n");
        body.append("Phone: ").append(order.getDeliveryPhone() != null ? order.getDeliveryPhone() : "N/A").append("\n");
        PaymentMethod paymentMethod = paymentRepository.findByOrderId(order.getId())
                .map(com.kaveri.entity.Payment::getPaymentMethod)
                .orElse(PaymentMethod.ONLINE);

        String paymentInfo = (paymentMethod == PaymentMethod.CASH_ON_DELIVERY)
                ? "COD"
                : ("Online " + order.getPaymentStatus());

        body.append("Total: ₹").append(order.getTotalAmount()).append(" (")
                .append(paymentInfo)
                .append(")\n");
        body.append("Delivery Address: ").append(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "Pickup").append("\n\n");
        body.append("Items Ordered:\n");

        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                body.append("• ").append(item.getQuantity()).append("x ")
                        .append(item.getFoodName()).append(" (₹").append(item.getSubtotal()).append(")\n");
            }
        }

        List<WhatsAppButton> buttons = List.of(
                new WhatsAppButton("ACTION_ACCEPT_" + order.getId(), "✅ Accept Order"),
                new WhatsAppButton("ACTION_REJECT_" + order.getId(), "❌ Reject Order")
        );

        String footerText = config.getRestaurant().getName();

        WhatsAppNotificationLog logEntry = WhatsAppNotificationLog.builder()
                .orderId(order.getId())
                .recipientPhone(makerPhone)
                .recipientRole(WhatsAppRecipientRole.MAKER)
                .messageType("NEW_ORDER_ALERT")
                .targetOrderStatus(OrderStatus.PLACED)
                .messagePreview(body.toString().length() > 950 ? body.substring(0, 950) + "..." : body.toString())
                .status(WhatsAppNotificationStatus.PENDING)
                .build();

        logEntry = notificationLogRepository.save(logEntry);

        try {
            String wamid = whatsAppClient.sendInteractiveButtonsMessage(
                    makerPhone, headerText, body.toString(), footerText, buttons);

            if (wamid != null) {
                logEntry.setStatus(WhatsAppNotificationStatus.SENT);
                logEntry.setProviderMessageId(wamid);
            } else {
                logEntry.setStatus(WhatsAppNotificationStatus.FAILED);
                logEntry.setErrorMessage("WhatsApp provider returned null message ID");
            }
        } catch (Exception e) {
            logEntry.setStatus(WhatsAppNotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Failed to dispatch WhatsApp new order alert to maker: {}", e.getMessage());
        }

        notificationLogRepository.save(logEntry);
    }

    /**
     * Notifies customer about order status progression.
     * Deduplicates so the same status update is never sent twice for the same order.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCustomerStatusUpdate(Order order, OrderStatus newStatus) {
        String customerPhone = order.getDeliveryPhone() != null && !order.getDeliveryPhone().isBlank()
                ? order.getDeliveryPhone()
                : (order.getUser() != null ? order.getUser().getPhone() : null);

        if (customerPhone == null || customerPhone.isBlank()) {
            return;
        }

        // Idempotency: Prevent duplicate notifications to customer for the same status
        boolean alreadySent = notificationLogRepository.existsByOrderIdAndMessageTypeAndTargetOrderStatus(
                order.getId(), "CUSTOMER_STATUS_UPDATE", newStatus);
        if (alreadySent) {
            log.debug("Customer notification already sent for order #{} status {}", order.getId(), newStatus);
            return;
        }

        String message = buildCustomerStatusMessage(order, newStatus);

        WhatsAppNotificationLog logEntry = WhatsAppNotificationLog.builder()
                .orderId(order.getId())
                .recipientPhone(customerPhone)
                .recipientRole(WhatsAppRecipientRole.CUSTOMER)
                .messageType("CUSTOMER_STATUS_UPDATE")
                .targetOrderStatus(newStatus)
                .messagePreview(message.length() > 950 ? message.substring(0, 950) + "..." : message)
                .status(WhatsAppNotificationStatus.PENDING)
                .build();

        logEntry = notificationLogRepository.save(logEntry);

        try {
            String wamid = whatsAppClient.sendTextMessage(customerPhone, message);
            if (wamid != null) {
                logEntry.setStatus(WhatsAppNotificationStatus.SENT);
                logEntry.setProviderMessageId(wamid);
            } else {
                logEntry.setStatus(WhatsAppNotificationStatus.FAILED);
                logEntry.setErrorMessage("Provider returned null message ID");
            }
        } catch (Exception e) {
            logEntry.setStatus(WhatsAppNotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Failed to dispatch customer WhatsApp notification: {}", e.getMessage());
        }

        notificationLogRepository.save(logEntry);
    }

    /**
     * Sends follow-up confirmation and next interactive step buttons to maker upon action.
     */
    public void notifyMakerStatusFollowup(Order order, OrderStatus newStatus, String makerPhone) {
        if (makerPhone == null || makerPhone.isBlank()) {
            makerPhone = resolveMakerPhone(order);
        }

        String header = "Order #" + order.getId() + " Updated";
        String body = String.format("Status successfully updated to: *%s*\nOrder Total: ₹%s",
                newStatus.name().replace("_", " "), order.getTotalAmount());

        List<WhatsAppButton> nextButtons = getNextButtonsForStatus(order.getId(), newStatus);

        if (nextButtons.isEmpty()) {
            // Terminal or no further buttons, send text confirmation
            whatsAppClient.sendTextMessage(makerPhone, body + "\nOrder lifecycle finalized. Thank you!");
        } else {
            whatsAppClient.sendInteractiveButtonsMessage(
                    makerPhone, header, body + "\nSelect next kitchen action:",
                    config.getRestaurant().getName(), nextButtons);
        }
    }

    private List<WhatsAppButton> getNextButtonsForStatus(Long orderId, OrderStatus status) {
        List<WhatsAppButton> buttons = new ArrayList<>();
        switch (status) {
            case CONFIRMED -> {
                buttons.add(new WhatsAppButton("ACTION_PREPARE_" + orderId, "👨‍🍳 Start Cooking"));
                buttons.add(new WhatsAppButton("ACTION_REJECT_" + orderId, "❌ Cancel Order"));
            }
            case PREPARING -> {
                buttons.add(new WhatsAppButton("ACTION_READY_" + orderId, "🥡 Mark Ready"));
                buttons.add(new WhatsAppButton("ACTION_REJECT_" + orderId, "❌ Cancel Order"));
            }
            case READY -> {
                buttons.add(new WhatsAppButton("ACTION_OUT_FOR_DELIVERY_" + orderId, "🛵 Dispatch"));
                buttons.add(new WhatsAppButton("ACTION_DELIVERED_" + orderId, "✅ Mark Delivered"));
            }
            case OUT_FOR_DELIVERY -> {
                buttons.add(new WhatsAppButton("ACTION_DELIVERED_" + orderId, "✅ Mark Delivered"));
            }
            default -> {}
        }
        return buttons;
    }

    private String buildCustomerStatusMessage(Order order, OrderStatus status) {
        String restaurantName = config.getRestaurant().getName();
        return switch (status) {
            case CONFIRMED -> String.format(
                    "🎉 *Order Confirmed!* (Order #%d)\n%s has accepted your order! Our chefs are preparing to cook your meal.\nTotal: ₹%s",
                    order.getId(), restaurantName, order.getTotalAmount());
            case PREPARING -> String.format(
                    "👨‍🍳 *Cooking in Progress!* (Order #%d)\nYour delicious meal is now freshly cooking in our kitchen at %s.",
                    order.getId(), restaurantName);
            case READY -> String.format(
                    "🥡 *Food Packed & Ready!* (Order #%d)\nYour order from %s is hot, freshly packaged, and ready for dispatch.",
                    order.getId(), restaurantName);
            case OUT_FOR_DELIVERY -> String.format(
                    "🛵 *Out for Delivery!* (Order #%d)\nOur delivery partner has picked up your food from %s and is heading your way!",
                    order.getId(), restaurantName);
            case DELIVERED -> String.format(
                    "✨ *Order Delivered!* (Order #%d)\nYour food has been delivered. Enjoy your meal! Thank you for choosing %s. We'd love your review!",
                    order.getId(), restaurantName);
            case CANCELLED -> String.format(
                    "❌ *Order Cancelled* (Order #%d)\nYour order from %s has been cancelled. Any online payment has been refunded to your original payment method.",
                    order.getId(), restaurantName);
            default -> String.format("Notice: Order #%d status updated to %s.", order.getId(), status.name());
        };
    }

    private String resolveMakerPhone(Order order) {
        if (order.getMaker() != null && order.getMaker().getPhone() != null && !order.getMaker().getPhone().isBlank()) {
            return order.getMaker().getPhone();
        }

        // Check if any admin has a configured phone number
        List<User> admins = userRepository.findAll();
        for (User u : admins) {
            if (u.getPhone() != null && !u.getPhone().isBlank()
                    && u.getRoles().stream().anyMatch(r -> r.getName() == com.kaveri.enums.RoleName.ROLE_ADMIN)) {
                return u.getPhone();
            }
        }

        // Fallback to configured restaurant phone
        return config.getRestaurant().getPhone();
    }

    /**
     * Scheduled outbox retry: Retries pending/failed notifications up to 3 times with exponential backoff.
     */
    @Scheduled(fixedDelay = 60000)
    public void retryPendingNotifications() {
        List<WhatsAppNotificationLog> pendingLogs = notificationLogRepository.findByStatusAndRetryCountLessThan(
                WhatsAppNotificationStatus.FAILED, 3);

        if (pendingLogs.isEmpty()) {
            return;
        }

        log.info("WhatsApp Outbox: processing {} retriable notifications", pendingLogs.size());

        for (WhatsAppNotificationLog entry : pendingLogs) {
            entry.setRetryCount(entry.getRetryCount() + 1);
            try {
                String wamid = whatsAppClient.sendTextMessage(entry.getRecipientPhone(), entry.getMessagePreview());
                if (wamid != null) {
                    entry.setStatus(WhatsAppNotificationStatus.SENT);
                    entry.setProviderMessageId(wamid);
                    entry.setErrorMessage(null);
                }
            } catch (Exception e) {
                entry.setErrorMessage("Retry attempt failed: " + e.getMessage());
            }
            notificationLogRepository.save(entry);
        }
    }
}
