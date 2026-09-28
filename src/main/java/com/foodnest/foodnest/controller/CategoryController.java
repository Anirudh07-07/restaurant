package com.foodnest.foodnest.controller;

import com.foodnest.foodnest.dto.response.ApiResponse;
import com.foodnest.foodnest.entity.Category;
import com.foodnest.foodnest.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public category listing")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Get all food categories (public)")
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        List<Category> categories = categoryRepository.findAll(Sort.by("name"));
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
