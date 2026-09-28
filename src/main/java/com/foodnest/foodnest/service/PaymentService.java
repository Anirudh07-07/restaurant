package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.PaymentVerifyRequest;
import com.foodnest.foodnest.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createRazorpayOrder(Long orderId);

    PaymentResponse verifyPayment(PaymentVerifyRequest request);
}
