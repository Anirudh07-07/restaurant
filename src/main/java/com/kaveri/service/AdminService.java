package com.kaveri.service;

import com.kaveri.dto.request.CategoryRequest;
import com.kaveri.dto.response.DashboardResponse;
import com.kaveri.dto.response.UserResponse;
import com.kaveri.entity.Category;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminService {

    DashboardResponse getDashboard();

    Page<UserResponse> getAllUsers(String keyword, int page, int size);

    UserResponse toggleUserStatus(Long userId);

    List<Category> getAllCategories();

    Category createCategory(CategoryRequest request);

    Category updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    // Contact messages
    Page<com.kaveri.entity.ContactMessage> getAllContactMessages(String keyword, Boolean isRead, int page, int size);

    com.kaveri.entity.ContactMessage toggleContactReadStatus(Long id);

    void deleteContactMessage(Long id);
}
