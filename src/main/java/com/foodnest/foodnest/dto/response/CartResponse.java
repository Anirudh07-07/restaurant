package com.foodnest.foodnest.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private int itemCount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemResponse {
        private Long cartItemId;
        private Long foodItemId;
        private String foodName;
        private String foodImageUrl;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal itemSubtotal;
        private boolean available;
    }
}
