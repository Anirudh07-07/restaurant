package com.kaveri.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Stored snapshot — food name at time of order.
     * Historical orders are not affected by menu changes.
     */
    @Column(name = "food_name", nullable = false, length = 150)
    private String foodName;

    /**
     * Price snapshot at time of order.
     */
    @Column(name = "food_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal foodPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "food_image_url", length = 500)
    private String foodImageUrl;
}
