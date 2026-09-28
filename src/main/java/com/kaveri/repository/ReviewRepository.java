package com.kaveri.repository;

import com.kaveri.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByFoodItemId(Long foodItemId, Pageable pageable);

    /** Restaurant-level reviews (foodItem is null) */
    Page<Review> findByFoodItemIsNull(Pageable pageable);

    Optional<Review> findByUserIdAndFoodItemId(Long userId, Long foodItemId);

    boolean existsByUserIdAndFoodItemId(Long userId, Long foodItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.foodItem.id = :foodItemId")
    Double getAverageRatingForFood(@Param("foodItemId") Long foodItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.foodItem IS NULL")
    Double getRestaurantAverageRating();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.foodItem IS NULL")
    long countRestaurantReviews();
}
