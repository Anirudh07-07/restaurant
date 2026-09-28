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
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Payments", description = "Razorpay payment integration")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    @Operation(summary = "Create Razorpay order to initiate payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long orderId) {
        PaymentResponse response = paymentService.createRazorpayOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Razorpay order created. Open checkout with the returned order ID."));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature (server-side)")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment verified successfully!"));
    }
}
