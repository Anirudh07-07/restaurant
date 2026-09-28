package com.kaveri.service.impl;

import com.kaveri.dto.request.ReviewRequest;
import com.kaveri.dto.response.ReviewResponse;
import com.kaveri.entity.FoodItem;
import com.kaveri.entity.Review;
import com.kaveri.entity.User;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.FoodItemRepository;
import com.kaveri.repository.ReviewRepository;
import com.kaveri.service.ReviewService;
import com.kaveri.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final FoodItemRepository foodItemRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User user = securityUtils.getCurrentUser();
        FoodItem foodItem = null;

        if (request.getFoodItemId() != null) {
            foodItem = foodItemRepository.findById(request.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", request.getFoodItemId()));

            // Prevent duplicate food review
            if (reviewRepository.existsByUserIdAndFoodItemId(user.getId(), request.getFoodItemId())) {
                throw new BadRequestException("You have already reviewed this item");
            }
        }

        Review review = Review.builder()
                .user(user)
                .foodItem(foodItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        // Update food item average rating
        if (foodItem != null) {
            updateFoodRating(foodItem);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        User user = securityUtils.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);

        if (review.getFoodItem() != null) {
            updateFoodRating(review.getFoodItem());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = securityUtils.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        if (!review.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        FoodItem foodItem = review.getFoodItem();
        reviewRepository.delete(review);

        if (foodItem != null) {
            updateFoodRating(foodItem);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getFoodReviews(Long foodItemId, int page, int size) {
        return reviewRepository.findByFoodItemId(
                foodItemId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getRestaurantReviews(int page, int size) {
        return reviewRepository.findByFoodItemIsNull(
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(int page, int size) {
        return reviewRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    private void updateFoodRating(FoodItem foodItem) {
        Double avg = reviewRepository.getAverageRatingForFood(foodItem.getId());
        if (avg != null) {
            foodItem.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        } else {
            foodItem.setRating(BigDecimal.ZERO);
        }
        // Count reviews
        long count = reviewRepository.findByFoodItemId(foodItem.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();
        foodItem.setReviewCount((int) count);
        foodItemRepository.save(foodItem);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .foodItemId(r.getFoodItem() != null ? r.getFoodItem().getId() : null)
                .foodItemName(r.getFoodItem() != null ? r.getFoodItem().getName() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
