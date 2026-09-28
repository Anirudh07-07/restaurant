package com.foodnest.foodnest.controller;

import com.foodnest.foodnest.dto.response.ApiResponse;
import com.foodnest.foodnest.dto.response.FoodItemResponse;
import com.foodnest.foodnest.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Public menu browsing APIs")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Get all available food items (paginated)")
    public ResponseEntity<ApiResponse<Page<FoodItemResponse>>> getMenu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                menuService.getMenu(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID")
    public ResponseEntity<ApiResponse<FoodItemResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getFoodById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter menu items")
    public ResponseEntity<ApiResponse<Page<FoodItemResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) Boolean spicy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                menuService.searchMenu(keyword, categoryId, minPrice, maxPrice,
                                       vegetarian, spicy, page, size, sortBy, sortDir)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get food items by category")
    public ResponseEntity<ApiResponse<Page<FoodItemResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                menuService.getByCategory(categoryId, page, size)));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated food items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getTopRated(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getTopRated(limit)));
    }
}
