package com.kaveri.service;

import com.kaveri.dto.request.PaymentVerifyRequest;
import com.kaveri.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createRazorpayOrder(Long orderId);

    PaymentResponse verifyPayment(PaymentVerifyRequest request);
}
