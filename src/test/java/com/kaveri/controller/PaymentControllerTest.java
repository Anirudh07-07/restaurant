package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.PaymentVerifyRequest;
import com.kaveri.dto.response.PaymentResponse;
import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.PaymentStatus;
import com.kaveri.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/payments/webhook - Should allow public callback without auth")
    void handleWebhook_publicCallback_returns200() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"payload\":{}}";
        doNothing().when(paymentService).processWebhook(any(String.class), any());

        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Razorpay-Signature", "sample_sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));
    }

    @Test
    @DisplayName("POST /api/payments/create/1 - Should reject unauthenticated user with 401/403")
    void createPayment_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/create/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/payments/create/1 - Authenticated customer can create order")
    void createPayment_authenticated_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .razorpayOrderId("order_mock_123")
                .amount(new BigDecimal("350.00"))
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayKeyId("test_key")
                .build();

        when(paymentService.createRazorpayOrder(eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_mock_123"))
                .andExpect(jsonPath("$.data.razorpayKeyId").value("test_key"));
    }

    @Test
    @WithMockUser(username = "customer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/payments/verify - Authenticated customer can verify payment")
    void verifyPayment_authenticated_returns200() throws Exception {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_mock_123")
                .razorpayPaymentId("pay_mock_456")
                .razorpaySignature("mock_sig")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .razorpayOrderId("order_mock_123")
                .transactionId("pay_mock_456")
                .amount(new BigDecimal("350.00"))
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(paymentService.verifyPayment(any(PaymentVerifyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }
}

