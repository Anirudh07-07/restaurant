package com.kaveri.service.impl;

import com.kaveri.dto.request.CategoryRequest;
import com.kaveri.dto.response.DashboardResponse;
import com.kaveri.dto.response.UserResponse;
import com.kaveri.entity.Category;
import com.kaveri.entity.ContactMessage;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.ReservationStatus;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.repository.*;
import com.kaveri.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIDNIGHT);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDate today = LocalDate.now();

        long totalOrders = orderRepository.count();
        long todaysOrders = orderRepository.countTodaysOrders(startOfDay, endOfDay);
        var totalRevenue = orderRepository.getTotalRevenue();
        long totalCustomers = userRepository.count();
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PLACED);
        
        long todaysReservations = reservationRepository.findByReservationDate(today, PageRequest.of(0, 1)).getTotalElements();
        long pendingReservations = reservationRepository.countByStatus(ReservationStatus.PENDING);
        Double avgRating = reviewRepository.getRestaurantAverageRating();
        long totalReviews = reviewRepository.countRestaurantReviews();

        long totalMenuItems = foodItemRepository.count();
        long itemsWithImages = foodItemRepository.countWithImages();
        long itemsWithoutImages = foodItemRepository.countWithoutImages();
        long totalCategories = categoryRepository.count();

        long unreadMessages = contactMessageRepository.countByIsReadFalse();
        long totalMessages = contactMessageRepository.count();

        return DashboardResponse.builder()
                .totalOrders(totalOrders)
                .todaysOrders(todaysOrders)
                .totalRevenue(totalRevenue)
                .totalCustomers(totalCustomers)
                .pendingOrders(pendingOrders)
                .todaysReservations(todaysReservations)
                .pendingReservations(pendingReservations)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews)
                .totalMenuItems(totalMenuItems)
                .itemsWithImages(itemsWithImages)
                .itemsWithoutImages(itemsWithoutImages)
                .totalCategories(totalCategories)
                .unreadMessages(unreadMessages)
                .totalMessages(totalMessages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, int page, int size) {
        return userRepository.searchUsers(
                keyword != null ? keyword : "",
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(u -> {
                    String role = u.getRoles().stream()
                            .findFirst().map(r -> r.getName().name()).orElse("ROLE_CUSTOMER");
                    return UserResponse.builder()
                            .id(u.getId())
                            .name(u.getName())
                            .email(u.getEmail())
                            .phone(u.getPhone())
                            .role(role)
                            .enabled(u.isEnabled())
                            .createdAt(u.getCreatedAt())
                            .build();
                });
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        String role = user.getRoles().stream()
                .findFirst().map(r -> r.getName().name()).orElse("ROLE_CUSTOMER");
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(role)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        if (foodItemRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category because active menu items are assigned to it. Please reassign or delete those food items first.");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessage> getAllContactMessages(String keyword, Boolean isRead, int page, int size) {
        return contactMessageRepository.searchMessages(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                isRead,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    @Override
    @Transactional
    public ContactMessage toggleContactReadStatus(Long id) {
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage", "id", id));
        msg.setRead(!msg.isRead());
        return contactMessageRepository.save(msg);
    }

    @Override
    @Transactional
    public void deleteContactMessage(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContactMessage", "id", id);
        }
        contactMessageRepository.deleteById(id);
    }
}
