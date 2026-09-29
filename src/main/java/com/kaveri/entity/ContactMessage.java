package com.kaveri.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 150)
    private String senderEmail;

    @Column(length = 25)
    private String senderPhone;

    @Column(length = 100)
    private String ventureDepartment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String senderMessage;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
