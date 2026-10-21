package com.kaveri.service;

import com.kaveri.dto.request.PaymentVerifyRequest;
import com.kaveri.dto.response.PaymentResponse;
import com.kaveri.entity.Order;
import com.kaveri.entity.Payment;
import com.kaveri.entity.Role;
import com.kaveri.entity.User;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.PaymentStatus;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.OrderRepository;
import com.kaveri.repository.PaymentRepository;
import com.kaveri.service.impl.PaymentServiceImpl;
import com.kaveri.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityUtils securityUtils;

    private PaymentServiceImpl paymentService;

    private User testUser;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                orderRepository,
                securityUtils,
                "test_key_id",
                "test_key_secret",
                "test_webhook_secret",
                true // Mock mode enabled for tests
        );

        testUser = User.builder()
                .id(1L)
                .name("Rohan Verma")
                .email("rohan@example.com")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_CUSTOMER).build()))
                .build();

        testOrder = Order.builder()
                .id(101L)
                .user(testUser)
                .totalAmount(new BigDecimal("500.00"))
                .orderStatus(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        testPayment = Payment.builder()
                .id(201L)
                .order(testOrder)
                .amount(new BigDecimal("500.00"))
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should create mock Razorpay order successfully in sandbox mode")
    void shouldCreateRazorpayOrderSuccessfully() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.of(testPayment));
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.createRazorpayOrder(101L);

        assertNotNull(response);
        assertEquals(101L, response.getOrderId());
        assertTrue(response.getRazorpayOrderId().startsWith("order_mock_"));
        assertEquals(new BigDecimal("500.00"), response.getAmount());
        assertEquals("rzp_test_mock_sandbox", response.getRazorpayKeyId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should block unauthorized user from creating payment for another user's order (IDOR Prevention)")
    void shouldBlockUnauthorizedUserFromOrderPayment() {
        User differentUser = User.builder()
                .id(999L)
                .email("attacker@example.com")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_CUSTOMER).build()))
                .build();

        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.getCurrentUser()).thenReturn(differentUser);

        assertThrows(UnauthorizedException.class, () -> paymentService.createRazorpayOrder(101L));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should verify mock payment and mark order and payment as PAID")
    void shouldVerifyMockPaymentSuccessfully() {
        testPayment.setPaymentId("order_mock_12345");
        when(paymentRepository.findByPaymentId("order_mock_12345")).thenReturn(Optional.of(testPayment));
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentVerifyRequest verifyReq = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_mock_12345")
                .razorpayPaymentId("pay_mock_99999")
                .razorpaySignature("mock_sig")
                .build();

        PaymentResponse response = paymentService.verifyPayment(verifyReq);

        assertNotNull(response);
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertEquals(PaymentStatus.PAID, testOrder.getPaymentStatus());
        assertEquals("pay_mock_99999", testPayment.getTransactionId());
        verify(paymentRepository).save(testPayment);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should process refund on a paid order")
    void shouldProcessRefundSuccessfully() {
        testPayment.setPaymentStatus(PaymentStatus.PAID);
        testPayment.setTransactionId("pay_12345");
        testOrder.setPaymentStatus(PaymentStatus.PAID);

        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(101L, "Customer cancellation");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getPaymentStatus());
        assertEquals(PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
        assertNotNull(response.getRefundId());
        assertTrue(response.getRefundId().startsWith("rfnd_mock_"));
        verify(paymentRepository).save(testPayment);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should process webhook payment.captured event and mark order as PAID")
    void shouldProcessWebhookCapturedEvent() {
        testPayment.setPaymentId("order_webhook_123");
        when(paymentRepository.findByPaymentId("order_webhook_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        String payload = "{" +
                "\"event\":\"payment.captured\"," +
                "\"payload\":{" +
                "  \"payment\":{" +
                "    \"entity\":{" +
                "      \"id\":\"pay_captured_789\"," +
                "      \"order_id\":\"order_webhook_123\"" +
                "    }" +
                "  }" +
                "}" +
                "}";

        paymentService.processWebhook(payload, "any_sig_in_mock");

        assertEquals(PaymentStatus.PAID, testPayment.getPaymentStatus());
        assertEquals("pay_captured_789", testPayment.getTransactionId());
        assertEquals(PaymentStatus.PAID, testOrder.getPaymentStatus());
        verify(paymentRepository).save(testPayment);
        verify(orderRepository).save(testOrder);
    }
}

