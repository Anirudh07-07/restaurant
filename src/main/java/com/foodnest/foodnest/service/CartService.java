package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.CartItemRequest;
import com.foodnest.foodnest.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItem(Long cartItemId, CartItemRequest request);

    CartResponse removeItem(Long cartItemId);

    void clearCart();
}
