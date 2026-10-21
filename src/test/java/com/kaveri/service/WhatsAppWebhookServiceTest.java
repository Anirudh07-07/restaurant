package com.kaveri.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.config.WhatsAppConfigProperties;
import com.kaveri.entity.Order;
import com.kaveri.entity.Role;
import com.kaveri.entity.User;
import com.kaveri.entity.WhatsAppWebhookEvent;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.OrderRepository;
import com.kaveri.repository.UserRepository;
import com.kaveri.repository.WhatsAppWebhookEventRepository;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceTest {

    @Mock
    private WhatsAppWebhookEventRepository webhookEventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WhatsAppNotificationService notificationService;

    private WhatsAppConfigProperties config;
    private ObjectMapper objectMapper;
    private WhatsAppWebhookService service;

    private User adminMaker;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        config = new WhatsAppConfigProperties();
        config.getWebhook().setVerifyToken("valid_verify_token");
        config.getWebhook().setAppSecret("test_app_secret");
        config.getRestaurant().setPhone("918235520520");
        config.getMock().setEnabled(false); // Enable signature checks for tests

        service = new WhatsAppWebhookService(
                config,
                webhookEventRepository,
                orderRepository,
                orderService,
                userRepository,
                notificationService,
                objectMapper
        );

        adminMaker = User.builder()
                .id(1L)
                .name("Kaveri Chef")
                .phone("918235520520")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_ADMIN).build()))
                .build();

        testOrder = Order.builder()
                .id(101L)
                .orderStatus(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("450.00"))
                .build();
    }

    @Test
    @DisplayName("Webhook challenge verification succeeds when mode and token match")
    void verifySubscription_success() {
        String challenge = service.verifySubscription("subscribe", "valid_verify_token", "11223344");
        assertEquals("11223344", challenge);
    }

    @Test
    @DisplayName("Webhook challenge fails when token does not match")
    void verifySubscription_invalidToken_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class, () ->
                service.verifySubscription("subscribe", "wrong_token", "11223344"));
    }

    @Test
    @DisplayName("Valid signature check succeeds; invalid signature throws UnauthorizedException")
    void validateSignature_checks() {
        String payload = "{\"test\":\"data\"}";
        String validSig = "sha256=" + new HmacUtils("HmacSHA256", "test_app_secret").hmacHex(payload);

        assertDoesNotThrow(() -> service.validateSignature(payload, validSig));
        assertThrows(UnauthorizedException.class, () -> service.validateSignature(payload, "sha256=invalid_hash"));
    }

    @Test
    @DisplayName("Authorized maker button reply ACTION_ACCEPT_101 updates order status to CONFIRMED")
    void processWebhook_buttonReplyAccept_updatesOrder() {
        String wamid = "wamid.TEST_101_ACCEPT";
        String payload = buildMetaWebhookPayload(wamid, "918235520520", "ACTION_ACCEPT_101", true);
        String signature = "sha256=" + new HmacUtils("HmacSHA256", "test_app_secret").hmacHex(payload);

        when(webhookEventRepository.existsByMessageId(wamid)).thenReturn(false);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));

        service.processWebhook(payload, signature);

        verify(orderService).updateOrderStatus(101L, OrderStatus.CONFIRMED);
        verify(webhookEventRepository).save(any(WhatsAppWebhookEvent.class));
        verify(notificationService).notifyMakerStatusFollowup(eq(testOrder), eq(OrderStatus.CONFIRMED), eq("918235520520"));
    }

    @Test
    @DisplayName("Duplicate webhook message ID is ignored idempotently")
    void processWebhook_duplicateMessageId_ignored() {
        String wamid = "wamid.DUPLICATE_MSG";
        String payload = buildMetaWebhookPayload(wamid, "918235520520", "ACTION_ACCEPT_101", true);
        String signature = "sha256=" + new HmacUtils("HmacSHA256", "test_app_secret").hmacHex(payload);

        when(webhookEventRepository.existsByMessageId(wamid)).thenReturn(true);

        service.processWebhook(payload, signature);

        verify(orderService, never()).updateOrderStatus(any(), any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unauthorized phone number cannot update order status")
    void processWebhook_unauthorizedPhone_rejected() {
        String wamid = "wamid.UNAUTH_USER";
        String attackerPhone = "919999999999";
        String payload = buildMetaWebhookPayload(wamid, attackerPhone, "ACTION_ACCEPT_101", true);
        String signature = "sha256=" + new HmacUtils("HmacSHA256", "test_app_secret").hmacHex(payload);

        when(webhookEventRepository.existsByMessageId(wamid)).thenReturn(false);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findAll()).thenReturn(List.of(adminMaker)); // admin phone is 918235520520

        service.processWebhook(payload, signature);

        verify(orderService, never()).updateOrderStatus(any(), any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Authorized maker text command 'PREPARE 101' updates order to PREPARING")
    void processWebhook_textCommand_updatesOrder() {
        String wamid = "wamid.TEXT_COMMAND";
        String payload = buildMetaWebhookPayload(wamid, "918235520520", "PREPARE 101", false);
        String signature = "sha256=" + new HmacUtils("HmacSHA256", "test_app_secret").hmacHex(payload);

        when(webhookEventRepository.existsByMessageId(wamid)).thenReturn(false);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));

        service.processWebhook(payload, signature);

        verify(orderService).updateOrderStatus(101L, OrderStatus.PREPARING);
    }

    private String buildMetaWebhookPayload(String messageId, String fromPhone, String action, boolean isInteractive) {
        if (isInteractive) {
            return "{" +
                    "\"object\":\"whatsapp_business_account\"," +
                    "\"entry\":[{" +
                    "  \"changes\":[{" +
                    "    \"value\":{" +
                    "      \"messages\":[{" +
                    "        \"from\":\"" + fromPhone + "\"," +
                    "        \"id\":\"" + messageId + "\"," +
                    "        \"type\":\"interactive\"," +
                    "        \"interactive\":{" +
                    "          \"type\":\"button_reply\"," +
                    "          \"button_reply\":{\"id\":\"" + action + "\",\"title\":\"Accept\"}" +
                    "        }" +
                    "      }]" +
                    "    }" +
                    "  }]" +
                    "}]}";
        } else {
            return "{" +
                    "\"object\":\"whatsapp_business_account\"," +
                    "\"entry\":[{" +
                    "  \"changes\":[{" +
                    "    \"value\":{" +
                    "      \"messages\":[{" +
                    "        \"from\":\"" + fromPhone + "\"," +
                    "        \"id\":\"" + messageId + "\"," +
                    "        \"type\":\"text\"," +
                    "        \"text\":{\"body\":\"" + action + "\"}" +
                    "      }]" +
                    "    }" +
                    "  }]" +
                    "}]}";
        }
    }
}
