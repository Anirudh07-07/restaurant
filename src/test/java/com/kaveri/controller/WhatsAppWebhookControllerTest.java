package com.kaveri.controller;

import com.kaveri.service.WhatsAppWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("WhatsAppWebhookController Integration Tests")
class WhatsAppWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WhatsAppWebhookService webhookService;

    @Test
    @DisplayName("GET /api/whatsapp/webhook - Valid subscription challenge returns 200 with challenge token")
    void verifyWebhook_validChallenge_returnsChallenge() throws Exception {
        when(webhookService.verifySubscription("subscribe", "test_verify_token", "challenge_123456"))
                .thenReturn("challenge_123456");

        mockMvc.perform(get("/api/whatsapp/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "test_verify_token")
                .param("hub.challenge", "challenge_123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge_123456"));

        verify(webhookService).verifySubscription("subscribe", "test_verify_token", "challenge_123456");
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook - Accepts Meta payload and acknowledges with EVENT_RECEIVED")
    void handleIncomingWebhook_validPayload_returnsEventReceived() throws Exception {
        String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
        doNothing().when(webhookService).processWebhook(any(String.class), any());

        mockMvc.perform(post("/api/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=test_sig")
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

        verify(webhookService).processWebhook(eq(payload), eq("sha256=test_sig"));
    }
}

