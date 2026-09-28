package com.kaveri.service;

import com.kaveri.dto.request.CartItemRequest;
import com.kaveri.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItem(Long cartItemId, CartItemRequest request);

    CartResponse removeItem(Long cartItemId);

    void clearCart();
}
