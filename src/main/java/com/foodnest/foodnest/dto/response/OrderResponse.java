package com.foodnest.foodnest.dto.response;

import com.foodnest.foodnest.enums.OrderStatus;
import com.foodnest.foodnest.enums.PaymentMethod;
import com.foodnest.foodnest.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String userName;

    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPincode;
    private String deliveryPhone;

    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;

    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;

    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private String foodName;
        private BigDecimal foodPrice;
        private int quantity;
        private BigDecimal subtotal;
        private String foodImageUrl;
    }
}
