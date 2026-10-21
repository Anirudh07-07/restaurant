package com.kaveri.service.impl;

import com.kaveri.dto.request.CartItemRequest;
import com.kaveri.dto.response.CartResponse;
import com.kaveri.entity.*;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.repository.*;
import com.kaveri.service.CartService;
import com.kaveri.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.tax.rate:0.05}")
    private BigDecimal taxRate;

    @Value("${app.delivery.fee:40.00}")
    private BigDecimal deliveryFee;

    @Value("${app.delivery.free-above:500.00}")
    private BigDecimal freeDeliveryAbove;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = securityUtils.getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .map(this::buildCartResponse)
                .orElseGet(() -> CartResponse.builder()
                        .cartId(null)
                        .items(Collections.emptyList())
                        .subtotal(BigDecimal.ZERO)
                        .tax(BigDecimal.ZERO)
                        .deliveryFee(BigDecimal.ZERO)
                        .total(BigDecimal.ZERO)
                        .itemCount(0)
                        .build());
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        // Fetch food item by ID if provided, otherwise by name
        FoodItem food;
        if (request.getFoodItemId() != null) {
            food = foodItemRepository.findById(request.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", request.getFoodItemId()));
        } else if (request.getFoodName() != null && !request.getFoodName().isBlank()) {
            food = foodItemRepository.findByNameIgnoreCase(request.getFoodName().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "name", request.getFoodName()));
        } else {
            throw new BadRequestException("Either food item ID or food name is required");
        }

        if (!food.isAvailable()) {
            throw new BadRequestException("'" + food.getName() + "' is currently unavailable");
        }

        // If item already in cart, increase quantity
        cartItemRepository.findByCartIdAndFoodItemId(cart.getId(), food.getId())
                .ifPresentOrElse(
                        existingItem -> {
                            int newQty = existingItem.getQuantity() + request.getQuantity();
                            if (newQty > 20) throw new BadRequestException("Maximum 20 units per item in cart");
                            existingItem.setQuantity(newQty);
                            existingItem.setPrice(food.getPrice()); // refresh price from DB
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            CartItem newItem = CartItem.builder()
                                     .cart(cart)
                                     .foodItem(food)
                                     .quantity(request.getQuantity())
                                     .price(food.getPrice()) // from DB, never from request
                                     .build();
                            cart.getItems().add(newItem);
                        }
                );

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long cartItemId, CartItemRequest request) {
        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (request.getQuantity() <= 0) {
            cart.getItems().remove(item);
        } else {
            // Refresh price from DB
            FoodItem food = foodItemRepository.findById(item.getFoodItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", item.getFoodItem().getId()));
            item.setQuantity(request.getQuantity());
            item.setPrice(food.getPrice());
        }

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long cartItemId) {
        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        if (!removed) {
            throw new ResourceNotFoundException("CartItem", "id", cartItemId);
        }

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart() {
        User user = securityUtils.getCurrentUser();
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = (cart.getItems() == null)
                ? Collections.emptyList()
                : cart.getItems().stream()
                .filter(item -> item != null && item.getFoodItem() != null)
                .map(item -> {
                    BigDecimal unitPrice = item.getPrice() != null ? item.getPrice()
                            : (item.getFoodItem().getPrice() != null ? item.getFoodItem().getPrice() : BigDecimal.ZERO);
                    int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;
                    return CartResponse.CartItemResponse.builder()
                            .cartItemId(item.getId())
                            .foodItemId(item.getFoodItem().getId())
                            .foodName(item.getFoodItem().getName())
                            .foodImageUrl(item.getFoodItem().getImageUrl())
                            .unitPrice(unitPrice)
                            .quantity(qty)
                            .itemSubtotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                            .available(item.getFoodItem().isAvailable())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal effectiveTaxRate = taxRate != null ? taxRate : new BigDecimal("0.05");
        BigDecimal effectiveDeliveryFee = deliveryFee != null ? deliveryFee : new BigDecimal("40.00");
        BigDecimal effectiveFreeDeliveryAbove = freeDeliveryAbove != null ? freeDeliveryAbove : new BigDecimal("500.00");

        BigDecimal tax = itemResponses.isEmpty() ? BigDecimal.ZERO
                : subtotal.multiply(effectiveTaxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal appliedDeliveryFee = (itemResponses.isEmpty() || subtotal.compareTo(effectiveFreeDeliveryAbove) >= 0)
                ? BigDecimal.ZERO : effectiveDeliveryFee;
        BigDecimal total = subtotal.add(tax).add(appliedDeliveryFee);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .tax(tax)
                .deliveryFee(appliedDeliveryFee)
                .total(total)
                .itemCount(itemResponses.size())
                .build();
    }
}
