package com.kaveri.controller;

import com.kaveri.dto.request.CategoryRequest;
import com.kaveri.dto.request.FoodItemRequest;
import com.kaveri.dto.response.*;
import com.kaveri.entity.Category;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.ReservationStatus;
import com.kaveri.service.*;
import com.kaveri.service.impl.MenuServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final MenuService menuService;
    private final OrderService orderService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final ImageUploadService imageUploadService;

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard stats")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    // ---- Menu Management ----

    @PostMapping("/menu")
    @Operation(summary = "Add a new food item")
    public ResponseEntity<ApiResponse<FoodItemResponse>> addFood(
            @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(menuService.createFood(request), "Food item created"));
    }

    @PutMapping("/menu/{id}")
    @Operation(summary = "Update food item")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateFood(
            @PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuService.updateFood(id, request)));
    }

    @DeleteMapping("/menu/{id}")
    @Operation(summary = "Delete food item")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {
        menuService.deleteFood(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Food item deleted"));
    }

    @PostMapping(value = "/menu/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload food image")
    public ResponseEntity<ApiResponse<FoodItemResponse>> uploadImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        String imageUrl = imageUploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(
                menuService.updateImageUrl(id, imageUrl), "Image uploaded"));
    }

    @PatchMapping("/menu/{id}/toggle-availability")
    @Operation(summary = "Toggle food item availability")
    public ResponseEntity<ApiResponse<FoodItemResponse>> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(menuService.toggleAvailability(id)));
    }

    // ---- Category Management ----

    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllCategories()));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a category")
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminService.createCategory(request), "Category created"));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    // ---- Order Management ----

    @GetMapping("/orders")
    @Operation(summary = "Get all orders (searchable, filterable)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(keyword, status, page, size)));
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id, @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrderStatus(id, status), "Order status updated"));
    }

    // ---- Customer Management ----

    @GetMapping("/users")
    @Operation(summary = "Get all customers")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAllUsers(keyword, page, size)));
    }

    @PatchMapping("/users/{id}/toggle-status")
    @Operation(summary = "Enable/disable a user account")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleUserStatus(id)));
    }

    // ---- Reservation Management ----

    @GetMapping("/reservations")
    @Operation(summary = "Get all reservations")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.getAllReservations(page, size)));
    }

    @PatchMapping("/reservations/{id}/status")
    @Operation(summary = "Update reservation status (CONFIRMED, REJECTED, COMPLETED)")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable Long id, @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.updateReservationStatus(id, status)));
    }

    // ---- Review Management ----

    @GetMapping("/reviews")
    @Operation(summary = "Get all reviews for moderation")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAllReviews(page, size)));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete a review (moderation)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Review removed"));
    }
}
