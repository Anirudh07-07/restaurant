package com.kaveri.controller;

import com.kaveri.dto.request.PaymentVerifyRequest;
import com.kaveri.dto.response.ApiResponse;
import com.kaveri.dto.response.PaymentResponse;
import com.kaveri.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment integration")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create Razorpay order to initiate payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long orderId) {
        PaymentResponse response = paymentService.createRazorpayOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Razorpay order created. Open checkout with the returned order ID."));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify Razorpay payment signature (server-side)")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment verified successfully!"));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay asynchronous webhook callback handler")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully"));
    }

    @PostMapping("/refund/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Refund an online payment (Admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long orderId,
            @RequestParam(required = false, defaultValue = "Administrative refund") String reason) {
        PaymentResponse response = paymentService.refundPayment(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment refunded successfully"));
    }
}
