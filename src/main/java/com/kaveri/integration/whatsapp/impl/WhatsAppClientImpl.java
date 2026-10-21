package com.kaveri.integration.whatsapp.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaveri.config.WhatsAppConfigProperties;
import com.kaveri.integration.whatsapp.WhatsAppClient;
import com.kaveri.integration.whatsapp.dto.WhatsAppButton;
import com.kaveri.util.PhoneNumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class WhatsAppClientImpl implements WhatsAppClient {

    private final WhatsAppConfigProperties config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WhatsAppClientImpl(WhatsAppConfigProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String sendTextMessage(String toPhone, String text) {
        String normalizedPhone = PhoneNumberUtils.normalize(toPhone);
        if (normalizedPhone.isEmpty()) {
            log.warn("Cannot send WhatsApp message: phone number is empty");
            return null;
        }

        if (!config.isConfigured() || config.getMock().isEnabled()) {
            String mockId = "wamid.mock_txt_" + System.currentTimeMillis();
            log.info("[MOCK WhatsApp Text] -> To: {} | WAMID: {}\nBody:\n{}", normalizedPhone, mockId, text);
            return mockId;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("messaging_product", "whatsapp");
            root.put("recipient_type", "individual");
            root.put("to", normalizedPhone);
            root.put("type", "text");

            ObjectNode textNode = root.putObject("text");
            textNode.put("preview_url", false);
            textNode.put("body", text);

            return executeWithRetry(root.toString(), normalizedPhone);

        } catch (Exception e) {
            log.error("Failed to construct/send WhatsApp text to {}: {}", normalizedPhone, e.getMessage());
            return null;
        }
    }

    @Override
    public String sendInteractiveButtonsMessage(
            String toPhone,
            String headerText,
            String bodyText,
            String footerText,
            List<WhatsAppButton> buttons) {

        String normalizedPhone = PhoneNumberUtils.normalize(toPhone);
        if (normalizedPhone.isEmpty()) {
            log.warn("Cannot send WhatsApp interactive message: phone number is empty");
            return null;
        }

        if (!config.isConfigured() || config.getMock().isEnabled()) {
            String mockId = "wamid.mock_btn_" + System.currentTimeMillis();
            log.info("[MOCK WhatsApp Interactive] -> To: {} | WAMID: {}\nHeader: {}\nBody: {}\nButtons: {}",
                    normalizedPhone, mockId, headerText, bodyText,
                    buttons.stream().map(b -> "[" + b.getTitle() + " -> " + b.getId() + "]").toList());
            return mockId;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("messaging_product", "whatsapp");
            root.put("recipient_type", "individual");
            root.put("to", normalizedPhone);
            root.put("type", "interactive");

            ObjectNode interactive = root.putObject("interactive");
            interactive.put("type", "button");

            if (headerText != null && !headerText.isBlank()) {
                ObjectNode header = interactive.putObject("header");
                header.put("type", "text");
                header.put("text", headerText);
            }

            ObjectNode body = interactive.putObject("body");
            body.put("text", bodyText);

            if (footerText != null && !footerText.isBlank()) {
                ObjectNode footer = interactive.putObject("footer");
                footer.put("text", footerText);
            }

            ObjectNode action = interactive.putObject("action");
            ArrayNode buttonsArray = action.putArray("buttons");

            // Meta allows up to 3 interactive buttons
            int limit = Math.min(buttons.size(), 3);
            for (int i = 0; i < limit; i++) {
                WhatsAppButton b = buttons.get(i);
                ObjectNode btn = buttonsArray.addObject();
                btn.put("type", "reply");
                ObjectNode reply = btn.putObject("reply");
                reply.put("id", b.getId());
                // Title max length 20 chars per WhatsApp API spec
                String title = b.getTitle();
                if (title.length() > 20) {
                    title = title.substring(0, 20);
                }
                reply.put("title", title);
            }

            return executeWithRetry(root.toString(), normalizedPhone);

        } catch (Exception e) {
            log.error("Failed to construct/send WhatsApp interactive buttons to {}: {}", normalizedPhone, e.getMessage());
            return null;
        }
    }

    private String executeWithRetry(String jsonPayload, String recipientPhone) {
        String endpoint = String.format("%s/%s/%s/messages",
                config.getApi().getBaseUrl(),
                config.getApi().getVersion(),
                config.getApi().getPhoneNumberId());

        int maxAttempts = 3;
        long backoffMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "Bearer " + config.getApi().getToken())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode respJson = objectMapper.readTree(response.body());
                    JsonNode messagesNode = respJson.path("messages");
                    if (messagesNode.isArray() && !messagesNode.isEmpty()) {
                        String wamid = messagesNode.get(0).path("id").asText();
                        log.info("WhatsApp message dispatched successfully to {} (WAMID: {})", recipientPhone, wamid);
                        return wamid;
                    }
                    return "wamid.generic_" + System.currentTimeMillis();
                }

                log.warn("Meta WhatsApp API call failed (attempt {}/{}). Status: {}, Response: {}",
                        attempt, maxAttempts, response.statusCode(), response.body());

                // If client error (e.g. 400 Bad Request, invalid phone), do not retry
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    break;
                }

            } catch (Exception e) {
                log.warn("WhatsApp dispatch error (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMs *= 2;
            }
        }

        return null;
    }
}

