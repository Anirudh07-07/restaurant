package com.foodnest.foodnest.service.impl;

import com.foodnest.foodnest.dto.response.NotificationResponse;
import com.foodnest.foodnest.entity.Notification;
import com.foodnest.foodnest.entity.User;
import com.foodnest.foodnest.repository.NotificationRepository;
import com.foodnest.foodnest.service.NotificationService;
import com.foodnest.foodnest.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public void createNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
        log.debug("Notification created for user {}: {}", user.getEmail(), title);
        // Future: trigger email/SMS/push notification here
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(int page, int size) {
        User user = securityUtils.getCurrentUser();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = securityUtils.getCurrentUser();
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAllRead() {
        User user = securityUtils.getCurrentUser();
        notificationRepository.markAllReadForUser(user.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
