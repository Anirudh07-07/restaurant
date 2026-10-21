package com.kaveri.repository;

import com.kaveri.entity.WhatsAppWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppWebhookEventRepository extends JpaRepository<WhatsAppWebhookEvent, Long> {

    boolean existsByMessageId(String messageId);

    Optional<WhatsAppWebhookEvent> findByMessageId(String messageId);
}

