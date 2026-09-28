package com.kaveri.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemRequest {

    @NotBlank(message = "Food name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "99999.99", message = "Price must not exceed 99999.99")
    private BigDecimal price;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private boolean available = true;
    private boolean vegetarian = false;
    private boolean spicy = false;

    @Min(value = 1, message = "Preparation time must be at least 1 minute")
    @Max(value = 120, message = "Preparation time must not exceed 120 minutes")
    private Integer preparationTime;
}
