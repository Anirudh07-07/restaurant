package com.kaveri.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /**
     * Price snapshot at the time item was added to cart.
     * Always re-fetched from DB at checkout to prevent price tampering.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
