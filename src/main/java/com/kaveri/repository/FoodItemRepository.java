package com.kaveri.repository;

import com.kaveri.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long>, JpaSpecificationExecutor<FoodItem> {

    Page<FoodItem> findByCategoryId(Long categoryId, Pageable pageable);

    Page<FoodItem> findByAvailableTrue(Pageable pageable);

    @Query("SELECT f FROM FoodItem f WHERE f.available = true " +
           "AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR f.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR f.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR f.price <= :maxPrice) " +
           "AND (:vegetarian IS NULL OR f.vegetarian = :vegetarian) " +
           "AND (:spicy IS NULL OR f.spicy = :spicy)")
    Page<FoodItem> searchMenu(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("vegetarian") Boolean vegetarian,
        @Param("spicy") Boolean spicy,
        Pageable pageable
    );

    @Query("SELECT f FROM FoodItem f WHERE f.available = true ORDER BY f.rating DESC")
    List<FoodItem> findTopRated(Pageable pageable);

    @Query("SELECT f FROM FoodItem f WHERE f.available = true AND f.category.id = :categoryId ORDER BY f.rating DESC")
    List<FoodItem> findTopRatedByCategory(@Param("categoryId") Long categoryId, Pageable pageable);
}
