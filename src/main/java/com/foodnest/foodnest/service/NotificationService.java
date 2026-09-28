package com.foodnest.foodnest.service;

import com.foodnest.foodnest.entity.User;

public interface NotificationService {

    void createNotification(User user, String title, String message);
}
