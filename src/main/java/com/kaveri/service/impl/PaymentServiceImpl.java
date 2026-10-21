package com.kaveri.service.impl;

import com.kaveri.dto.response.PaymentResponse;
import com.kaveri.dto.request.PaymentVerifyRequest;
import com.kaveri.entity.Order;
import com.kaveri.entity.Payment;
import com.kaveri.entity.User;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.PaymentStatus;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.PaymentException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.OrderRepository;
import com.kaveri.repository.PaymentRepository;
import com.kaveri.service.PaymentService;
import com.kaveri.util.SecurityUtils;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;
    private final boolean mockEnabled;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            SecurityUtils securityUtils,
            @Value("${razorpay.key.id:}") String razorpayKeyId,
            @Value("${razorpay.key.secret:}") String razorpayKeySecret,
            @Value("${razorpay.webhook.secret:}") String razorpayWebhookSecret,
            @Value("${razorpay.mock.enabled:false}") boolean mockEnabled) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.securityUtils = securityUtils;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
        this.mockEnabled = mockEnabled || isDummyKey(razorpayKeyId);
    }

    private static boolean isDummyKey(String key) {
        return key == null || key.isBlank() || key.contains("your_") || key.equals("dummy");
    }

    @Override
    @Transactional
    public PaymentResponse createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // IDOR Check: Ensure authenticated user owns the order (or is Admin)
        validateOrderOwnership(order);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record", "orderId", orderId));

        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid.");
        }

        if (mockEnabled) {
            log.info("Razorpay mock mode active. Generating sandbox order for order #{}", orderId);
            String mockOrderId = "order_mock_" + System.currentTimeMillis() + "_" + orderId;
            payment.setPaymentId(mockOrderId);
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .id(payment.getId())
                    .orderId(orderId)
                    .razorpayOrderId(mockOrderId)
                    .amount(order.getTotalAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentStatus(payment.getPaymentStatus())
                    .razorpayKeyId("rzp_test_mock_sandbox")
                    .build();
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Amount in paise (smallest currency unit)
            int amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "kaveri_order_" + orderId);

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
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to create payment order. Please try again.", e);
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByPaymentId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "razorpayOrderId",
                        request.getRazorpayOrderId()));

        Order order = payment.getOrder();
        validateOrderOwnership(order);

        // In mock mode or mock order id
        if (mockEnabled || (request.getRazorpayOrderId() != null && request.getRazorpayOrderId().startsWith("order_mock_"))) {
            log.info("Razorpay mock mode: approving payment for order #{}", order.getId());
            payment.setTransactionId(request.getRazorpayPaymentId() != null
                    ? request.getRazorpayPaymentId()
                    : "pay_mock_" + System.currentTimeMillis());
            payment.setPaymentStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            return toResponse(payment);
        }

        // Server-side signature verification — NEVER trust frontend success data
        String expectedSignature = generateSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId()
        );

        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            log.error("Payment signature mismatch for order ID: {}", request.getRazorpayOrderId());
            throw new PaymentException("Payment verification failed: Invalid signature");
        }

        // Update payment record
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setPaymentStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        // Update order payment status
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("Payment verified successfully for order #{}", order.getId());
        return toResponse(payment);
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        log.info("Processing Razorpay webhook callback");

        // Validate webhook signature if secret configured
        if (!mockEnabled && razorpayWebhookSecret != null && !razorpayWebhookSecret.isBlank()) {
            if (signatureHeader == null || signatureHeader.isBlank()) {
                throw new PaymentException("Missing webhook signature header");
            }
            String expectedSignature = new HmacUtils("HmacSHA256", razorpayWebhookSecret).hmacHex(payload);
            if (!expectedSignature.equals(signatureHeader)) {
                log.error("Webhook signature mismatch!");
                throw new PaymentException("Invalid webhook signature");
            }
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");
            JSONObject payloadObj = json.optJSONObject("payload");
            if (payloadObj == null) {
                log.warn("Webhook payload empty: {}", payload);
                return;
            }

            JSONObject paymentObj = payloadObj.optJSONObject("payment");
            if (paymentObj == null) {
                return;
            }
            JSONObject entity = paymentObj.getJSONObject("entity");
            String razorpayOrderId = entity.optString("order_id");
            String razorpayPaymentId = entity.optString("id");

            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                return;
            }

            paymentRepository.findByPaymentId(razorpayOrderId).ifPresent(payment -> {
                Order order = payment.getOrder();
                if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                    if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                        payment.setTransactionId(razorpayPaymentId);
                        payment.setPaymentStatus(PaymentStatus.PAID);
                        paymentRepository.save(payment);

                        order.setPaymentStatus(PaymentStatus.PAID);
                        orderRepository.save(order);
                        log.info("Webhook updated Order #{} to PAID via event {}", order.getId(), event);
                    }
                } else if ("payment.failed".equals(event)) {
                    if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                        payment.setPaymentStatus(PaymentStatus.FAILED);
                        payment.setFailureReason(entity.optString("error_description", "Payment failed"));
                        paymentRepository.save(payment);

                        order.setPaymentStatus(PaymentStatus.FAILED);
                        orderRepository.save(order);
                        log.warn("Webhook recorded payment failure for Order #{}: {}",
                                order.getId(), payment.getFailureReason());
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload: {}", e.getMessage(), e);
            throw new PaymentException("Webhook processing failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record", "orderId", orderId));

        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("No online payment to refund for order #{}. Status: {}", orderId, payment.getPaymentStatus());
            return toResponse(payment);
        }

        if (mockEnabled || isDummyKey(razorpayKeyId)) {
            String mockRefundId = "rfnd_mock_" + System.currentTimeMillis();
            payment.setRefundId(mockRefundId);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);
            log.info("Mock refund processed for order #{}: {}", orderId, mockRefundId);
            return toResponse(payment);
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject refundRequest = new JSONObject();
            int amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();
            refundRequest.put("amount", amountInPaise);
            refundRequest.put("speed", "optimum");
            JSONObject notes = new JSONObject();
            notes.put("reason", reason != null ? reason : "Order cancellation");
            notes.put("orderId", String.valueOf(orderId));
            refundRequest.put("notes", notes);

            com.razorpay.Refund refund = client.payments.refund(payment.getTransactionId(), refundRequest);
            String refundId = refund.get("id");

            payment.setRefundId(refundId);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);

            log.info("Razorpay refund processed for order #{}: {}", orderId, refundId);
            return toResponse(payment);

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for order #{}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to initiate Razorpay refund: " + e.getMessage(), e);
        }
    }

    private void validateOrderOwnership(Order order) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = currentUser.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
            if (!isAdmin && !order.getUser().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to access payment for this order");
            }
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not retrieve current user context: {}", e.getMessage());
        }
    }

    private String generateSignature(String razorpayOrderId, String razorpayPaymentId) {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        return new HmacUtils("HmacSHA256", razorpayKeySecret).hmacHex(payload);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .razorpayOrderId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .refundId(payment.getRefundId())
                .failureReason(payment.getFailureReason())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .razorpayKeyId(razorpayKeyId)
                .build();
    }
}
