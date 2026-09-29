package com.kaveri.service.impl;

import com.kaveri.dto.request.FoodItemRequest;
import com.kaveri.dto.response.FoodItemResponse;
import com.kaveri.entity.Category;
import com.kaveri.entity.FoodItem;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.repository.CategoryRepository;
import com.kaveri.repository.FoodItemRepository;
import com.kaveri.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemResponse> getMenu(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return foodItemRepository.findByAvailableTrue(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodItemResponse getFoodById(Long id) {
        return foodItemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemResponse> searchMenu(
            String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            Boolean vegetarian, Boolean spicy, int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sanitizeSortField(sortBy)).descending()
                : Sort.by(sanitizeSortField(sortBy)).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return foodItemRepository.searchMenu(
                keyword, categoryId, minPrice, maxPrice, vegetarian, spicy, pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemResponse> getByCategory(Long categoryId, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("rating").descending());
        return foodItemRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getTopRated(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return foodItemRepository.findTopRated(pageable)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodItemResponse createFood(FoodItemRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        FoodItem item = FoodItem.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .available(request.isAvailable())
                .vegetarian(request.isVegetarian())
                .spicy(request.isSpicy())
                .preparationTime(request.getPreparationTime())
                .build();

        return toResponse(foodItemRepository.save(item));
    }

    @Override
    @Transactional
    public FoodItemResponse updateFood(Long id, FoodItemRequest request) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        item.setAvailable(request.isAvailable());
        item.setVegetarian(request.isVegetarian());
        item.setSpicy(request.isSpicy());
        item.setPreparationTime(request.getPreparationTime());

        return toResponse(foodItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteFood(Long id) {
        if (!foodItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("FoodItem", "id", id);
        }
        foodItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public FoodItemResponse updateImageUrl(Long id, String imageUrl) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        String oldImageUrl = item.getImageUrl();
        item.setImageUrl(imageUrl);
        FoodItem saved = foodItemRepository.save(item);

        // Safe cleanup: If the old image was an uploaded file, delete it only if no other items reference it
        if (oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
            cleanupUploadedImageIfUnused(oldImageUrl);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public FoodItemResponse removeImageUrl(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        String oldImageUrl = item.getImageUrl();
        item.setImageUrl(null);
        FoodItem saved = foodItemRepository.save(item);

        if (oldImageUrl != null) {
            cleanupUploadedImageIfUnused(oldImageUrl);
        }

        return toResponse(saved);
    }

    private void cleanupUploadedImageIfUnused(String oldImageUrl) {
        if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
            try {
                long count = foodItemRepository.countByImageUrl(oldImageUrl);
                if (count == 0) {
                    String filename = oldImageUrl.replaceFirst("^/uploads/", "");
                    Path filePath = Paths.get(uploadDir, filename).toAbsolutePath().normalize();
                    Files.deleteIfExists(filePath);
                    log.info("Deleted orphaned uploaded image file: {}", filePath);
                }
            } catch (Exception e) {
                log.warn("Failed to delete unused uploaded image file {}: {}", oldImageUrl, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public FoodItemResponse toggleAvailability(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        item.setAvailable(!item.isAvailable());
        return toResponse(foodItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemResponse> searchAdminMenu(
            String keyword, Long categoryId, Boolean vegetarian,
            Boolean available, Boolean hasImage, int page, int size,
            String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sanitizeSortField(sortBy)).descending()
                : Sort.by(sanitizeSortField(sortBy)).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return foodItemRepository.searchAdminMenu(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                categoryId,
                vegetarian,
                available,
                hasImage,
                pageable
        ).map(this::toResponse);
    }

    public FoodItemResponse toResponse(FoodItem item) {
        return FoodItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .available(item.isAvailable())
                .vegetarian(item.isVegetarian())
                .spicy(item.isSpicy())
                .preparationTime(item.getPreparationTime())
                .rating(item.getRating())
                .reviewCount(item.getReviewCount())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private String sanitizeSortField(String sortBy) {
        return switch (sortBy) {
            case "price", "rating", "preparationTime", "createdAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
