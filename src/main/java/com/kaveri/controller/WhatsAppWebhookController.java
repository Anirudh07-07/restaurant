package com.kaveri.controller;

import com.kaveri.service.WhatsAppWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/whatsapp/webhook", "/api/webhook/whatsapp"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp Webhook", description = "Meta WhatsApp Business Platform Cloud API Webhook Integration")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;

    @GetMapping
    @Operation(summary = "Verify Meta WhatsApp Webhook Subscription Challenge")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta WhatsApp webhook verification request: mode={}, token={}", mode, token);
        String responseChallenge = webhookService.verifySubscription(mode, token, challenge);
        return ResponseEntity.ok(responseChallenge);
    }

    @PostMapping
    @Operation(summary = "Receive and Process WhatsApp Message and Button Reply Events")
    public ResponseEntity<String> handleIncomingWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        log.debug("Received Meta WhatsApp incoming event payload");
        webhookService.processWebhook(payload, signature);
        // Meta requires a prompt 200 OK acknowledgment
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}

