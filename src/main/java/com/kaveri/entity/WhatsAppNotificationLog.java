package com.kaveri.entity;

import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.WhatsAppNotificationStatus;
import com.kaveri.enums.WhatsAppRecipientRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "whatsapp_notification_logs",
    indexes = {
        @Index(name = "idx_wa_order_id", columnList = "order_id"),
        @Index(name = "idx_wa_status", columnList = "status"),
        @Index(name = "idx_wa_recipient", columnList = "recipient_phone")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "recipient_phone", nullable = false, length = 25)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 20)
    private WhatsAppRecipientRole recipientRole;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_order_status", length = 30)
    private OrderStatus targetOrderStatus;

    @Column(name = "message_preview", length = 1000)
    private String messagePreview;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WhatsAppNotificationStatus status = WhatsAppNotificationStatus.PENDING;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

