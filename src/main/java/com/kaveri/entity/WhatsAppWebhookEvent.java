package com.kaveri.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "whatsapp_webhook_events",
    indexes = {
        @Index(name = "idx_wa_event_msg_id", columnList = "message_id", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 150)
    private String messageId;

    @Column(name = "sender_phone", nullable = false, length = 25)
    private String senderPhone;

    @Column(name = "action_payload", length = 100)
    private String actionPayload;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "status_applied", length = 30)
    private String statusApplied;

    @CreatedDate
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}

