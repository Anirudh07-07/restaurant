package com.foodnest.foodnest.service.impl;

import com.foodnest.foodnest.dto.response.PaymentResponse;
import com.foodnest.foodnest.dto.request.PaymentVerifyRequest;
import com.foodnest.foodnest.entity.Order;
import com.foodnest.foodnest.entity.Payment;
import com.foodnest.foodnest.enums.PaymentStatus;
import com.foodnest.foodnest.exception.PaymentException;
import com.foodnest.foodnest.exception.ResourceNotFoundException;
import com.foodnest.foodnest.repository.OrderRepository;
import com.foodnest.foodnest.repository.PaymentRepository;
import com.foodnest.foodnest.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            @Value("${razorpay.key.id}") String razorpayKeyId,
            @Value("${razorpay.key.secret}") String razorpayKeySecret) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Override
    @Transactional
    public PaymentResponse createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record", "orderId", orderId));

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Amount in paise (smallest currency unit)
            int amountInPaise = order.getTotalAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "foodnest_order_" + orderId);

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Store Razorpay order ID in payment record
            payment.setPaymentId(razorpayOrderId);
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .id(payment.getId())
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(order.getTotalAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentStatus(payment.getPaymentStatus())
                    .razorpayKeyId(razorpayKeyId) // safe to expose public key
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to create payment order. Please try again.", e);
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        // 1. Find payment record by Razorpay order ID
        Payment payment = paymentRepository.findByPaymentId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "razorpayOrderId",
                        request.getRazorpayOrderId()));

        // 2. Server-side signature verification — NEVER trust frontend success data
        String expectedSignature = generateSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId()
        );

        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            log.error("Payment signature mismatch for order ID: {}", request.getRazorpayOrderId());
            throw new PaymentException("Payment verification failed: Invalid signature");
        }

        // 3. Update payment record
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setPaymentStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        // 4. Update order payment status
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("Payment verified successfully for order #{}", order.getId());

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .razorpayOrderId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    /**
     * Generates HMAC SHA256 signature.
     * Format: razorpay_order_id|razorpay_payment_id
     */
    private String generateSignature(String razorpayOrderId, String razorpayPaymentId) {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        return new HmacUtils("HmacSHA256", razorpayKeySecret).hmacHex(payload);
    }
}
