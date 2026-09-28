package com.kaveri.service;

import com.kaveri.dto.request.ReviewRequest;
import com.kaveri.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request);

    ReviewResponse updateReview(Long reviewId, ReviewRequest request);

    void deleteReview(Long reviewId);

    Page<ReviewResponse> getFoodReviews(Long foodItemId, int page, int size);

    Page<ReviewResponse> getRestaurantReviews(int page, int size);

    Page<ReviewResponse> getAllReviews(int page, int size);
}
