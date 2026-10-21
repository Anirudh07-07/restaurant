package com.kaveri.service;

import com.kaveri.config.WhatsAppConfigProperties;
import com.kaveri.entity.*;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.PaymentStatus;
import com.kaveri.enums.WhatsAppNotificationStatus;
import com.kaveri.enums.WhatsAppRecipientRole;
import com.kaveri.integration.whatsapp.WhatsAppClient;
import com.kaveri.repository.PaymentRepository;
import com.kaveri.repository.UserRepository;
import com.kaveri.repository.WhatsAppNotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {

    @Mock
    private WhatsAppClient whatsAppClient;

    @Mock
    private WhatsAppNotificationLogRepository notificationLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private WhatsAppConfigProperties config;
    private WhatsAppNotificationService service;

    private Order testOrder;
    private User customer;

    @BeforeEach
    void setUp() {
        config = new WhatsAppConfigProperties();
        config.getRestaurant().setPhone("918235520520");
        config.getRestaurant().setName("Kaveri Kitchen");

        service = new WhatsAppNotificationService(
                whatsAppClient,
                config,
                notificationLogRepository,
                userRepository,
                paymentRepository
        );

        customer = User.builder()
                .id(10L)
                .name("Rohan Verma")
                .phone("919876543210")
                .build();

        testOrder = Order.builder()
                .id(501L)
                .user(customer)
                .deliveryAddress("Main Road, Ranchi")
                .deliveryPhone("919876543210")
                .totalAmount(new BigDecimal("650.00"))
                .orderStatus(OrderStatus.PLACED)
                .orderItems(List.of(
                        OrderItem.builder().foodName("Paneer Butter Masala").quantity(2).subtotal(new BigDecimal("500.00")).build(),
                        OrderItem.builder().foodName("Butter Naan").quantity(3).subtotal(new BigDecimal("150.00")).build()
                ))
                .build();
    }

    @Test
    @DisplayName("notifyMakerNewOrder sends interactive buttons to maker and records outbox log as SENT")
    void notifyMakerNewOrder_success() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(
                Payment.builder().paymentMethod(PaymentMethod.ONLINE).paymentStatus(PaymentStatus.PAID).build()
        ));
        when(whatsAppClient.sendInteractiveButtonsMessage(any(), any(), any(), any(), any()))
                .thenReturn("wamid.MOCK_ORDER_501");
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.notifyMakerNewOrder(testOrder);

        ArgumentCaptor<WhatsAppNotificationLog> captor = ArgumentCaptor.forClass(WhatsAppNotificationLog.class);
        verify(notificationLogRepository, atLeastOnce()).save(captor.capture());

        WhatsAppNotificationLog savedLog = captor.getValue();
        assertEquals(501L, savedLog.getOrderId());
        assertEquals("918235520520", savedLog.getRecipientPhone());
        assertEquals(WhatsAppRecipientRole.MAKER, savedLog.getRecipientRole());
        assertEquals(WhatsAppNotificationStatus.SENT, savedLog.getStatus());
        assertEquals("wamid.MOCK_ORDER_501", savedLog.getProviderMessageId());
    }

    @Test
    @DisplayName("notifyCustomerStatusUpdate sends customer message and deduplicates subsequent identical notifications")
    void notifyCustomerStatusUpdate_deduplicates() {
        when(notificationLogRepository.existsByOrderIdAndMessageTypeAndTargetOrderStatus(
                501L, "CUSTOMER_STATUS_UPDATE", OrderStatus.CONFIRMED)).thenReturn(false);

        when(whatsAppClient.sendTextMessage(eq("919876543210"), anyString()))
                .thenReturn("wamid.CUSTOMER_CONFIRM");
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        // First call should dispatch
        service.notifyCustomerStatusUpdate(testOrder, OrderStatus.CONFIRMED);

        verify(whatsAppClient, times(1)).sendTextMessage(eq("919876543210"), anyString());

        // Subsequent call should be suppressed by deduplication
        when(notificationLogRepository.existsByOrderIdAndMessageTypeAndTargetOrderStatus(
                501L, "CUSTOMER_STATUS_UPDATE", OrderStatus.CONFIRMED)).thenReturn(true);

        service.notifyCustomerStatusUpdate(testOrder, OrderStatus.CONFIRMED);

        verify(whatsAppClient, times(1)).sendTextMessage(eq("919876543210"), anyString());
    }

    @Test
    @DisplayName("retryPendingNotifications retries failed notifications and marks them SENT on success")
    void retryPendingNotifications_success() {
        WhatsAppNotificationLog failedLog = WhatsAppNotificationLog.builder()
                .id(1L)
                .orderId(501L)
                .recipientPhone("918235520520")
                .status(WhatsAppNotificationStatus.FAILED)
                .retryCount(1)
                .messagePreview("Retry test body")
                .build();

        when(notificationLogRepository.findByStatusAndRetryCountLessThan(WhatsAppNotificationStatus.FAILED, 3))
                .thenReturn(List.of(failedLog));
        when(whatsAppClient.sendTextMessage("918235520520", "Retry test body"))
                .thenReturn("wamid.RETRY_SUCCESS");

        service.retryPendingNotifications();

        assertEquals(WhatsAppNotificationStatus.SENT, failedLog.getStatus());
        assertEquals("wamid.RETRY_SUCCESS", failedLog.getProviderMessageId());
        assertEquals(2, failedLog.getRetryCount());
        verify(notificationLogRepository).save(failedLog);
    }
}

