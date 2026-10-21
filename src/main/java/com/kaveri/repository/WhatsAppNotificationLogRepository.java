package com.kaveri.repository;

import com.kaveri.entity.WhatsAppNotificationLog;
import com.kaveri.enums.WhatsAppNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppNotificationLogRepository extends JpaRepository<WhatsAppNotificationLog, Long> {

    List<WhatsAppNotificationLog> findByOrderId(Long orderId);

    List<WhatsAppNotificationLog> findByStatusAndRetryCountLessThan(
            WhatsAppNotificationStatus status, int maxRetries);

    boolean existsByOrderIdAndMessageTypeAndTargetOrderStatus(
            Long orderId, String messageType, com.kaveri.enums.OrderStatus targetOrderStatus);
}

