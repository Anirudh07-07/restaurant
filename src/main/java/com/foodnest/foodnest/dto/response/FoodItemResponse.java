package com.foodnest.foodnest.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private boolean available;
    private boolean vegetarian;
    private boolean spicy;
    private Integer preparationTime;
    private BigDecimal rating;
    private int reviewCount;
    private LocalDateTime createdAt;
}
