package com.kaveri.service;

import com.kaveri.dto.response.NotificationResponse;
import com.kaveri.entity.User;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void createNotification(User user, String title, String message);

    Page<NotificationResponse> getMyNotifications(int page, int size);

    long getUnreadCount();

    void markAllRead();
}
