package com.foodnest.foodnest.controller;

import com.foodnest.foodnest.dto.request.ReviewRequest;
import com.foodnest.foodnest.dto.response.ApiResponse;
import com.foodnest.foodnest.dto.response.ReviewResponse;
import com.foodnest.foodnest.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Food and restaurant reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/food/{foodItemId}")
    @Operation(summary = "Get reviews for a specific food item (public)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getFoodReviews(
            @PathVariable Long foodItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getFoodReviews(foodItemId, page, size)));
    }

    @GetMapping("/restaurant")
    @Operation(summary = "Get restaurant reviews (public)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getRestaurantReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getRestaurantReviews(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.createReview(request), "Review submitted"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update your review")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.updateReview(id, request), "Review updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a review (own review or admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
    }
}
