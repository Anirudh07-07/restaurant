package com.kaveri.service;

import com.kaveri.dto.request.FoodItemRequest;
import com.kaveri.dto.response.FoodItemResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface MenuService {

    Page<FoodItemResponse> getMenu(int page, int size, String sortBy, String sortDir);

    FoodItemResponse getFoodById(Long id);

    Page<FoodItemResponse> searchMenu(String keyword, Long categoryId, BigDecimal minPrice,
                                      BigDecimal maxPrice, Boolean vegetarian, Boolean spicy,
                                      int page, int size, String sortBy, String sortDir);

    Page<FoodItemResponse> getByCategory(Long categoryId, int page, int size);

    List<FoodItemResponse> getTopRated(int limit);

    FoodItemResponse createFood(FoodItemRequest request);

    FoodItemResponse updateFood(Long id, FoodItemRequest request);

    void deleteFood(Long id);

    FoodItemResponse updateImageUrl(Long id, String imageUrl);

    FoodItemResponse removeImageUrl(Long id);

    FoodItemResponse toggleAvailability(Long id);

    Page<FoodItemResponse> searchAdminMenu(
            String keyword, Long categoryId, Boolean vegetarian,
            Boolean available, Boolean hasImage, int page, int size,
            String sortBy, String sortDir
    );
}
