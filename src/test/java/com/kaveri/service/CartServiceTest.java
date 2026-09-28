package com.kaveri.service;

import com.kaveri.dto.request.CartItemRequest;
import com.kaveri.dto.response.CartResponse;
import com.kaveri.entity.*;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.BadRequestException;
import com.kaveri.repository.CartItemRepository;
import com.kaveri.repository.CartRepository;
import com.kaveri.repository.FoodItemRepository;
import com.kaveri.service.impl.CartServiceImpl;
import com.kaveri.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private FoodItem testFood;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "taxRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(cartService, "deliveryFee", new BigDecimal("40.00"));
        ReflectionTestUtils.setField(cartService, "freeDeliveryAbove", new BigDecimal("500.00"));

        testUser = User.builder().id(1L).name("Test").email("test@test.com")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_CUSTOMER).build())).build();

        testCart = Cart.builder().id(1L).user(testUser).items(new ArrayList<>()).build();

        Category cat = Category.builder().id(1L).name("Pizza").build();
        testFood = FoodItem.builder()
                .id(10L).name("Margherita").price(new BigDecimal("299"))
                .category(cat).available(true).build();
    }

    @Test
    @DisplayName("Add item to cart - Success")
    void addItem_toCart_shouldSucceed() {
        CartItemRequest request = new CartItemRequest(10L, 2);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(testFood));
        when(cartItemRepository.findByCartIdAndFoodItemId(1L, 10L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartResponse response = cartService.addItem(request);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Add unavailable item - Should throw BadRequestException")
    void addItem_unavailableFood_shouldThrow() {
        testFood.setAvailable(false);
        CartItemRequest request = new CartItemRequest(10L, 1);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(testFood));

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("Get cart - Calculate totals correctly")
    void getCart_shouldCalculateTotalsCorrectly() {
        // Add an item manually to test total calculation
        CartItem item = CartItem.builder()
                .id(1L).cart(testCart).foodItem(testFood)
                .quantity(2).price(new BigDecimal("299")).build();
        testCart.getItems().add(item);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        CartResponse response = cartService.getCart();

        // Subtotal = 299 * 2 = 598
        // Tax = 598 * 0.05 = 29.90
        // Delivery = FREE (above 500)
        // Total = 598 + 29.90 = 627.90
        assertThat(response.getSubtotal()).isEqualByComparingTo("598.00");
        assertThat(response.getTax()).isEqualByComparingTo("29.90");
        assertThat(response.getDeliveryFee()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo("627.90");
    }

    @Test
    @DisplayName("Clear cart - Should remove all items")
    void clearCart_shouldRemoveAllItems() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any())).thenReturn(testCart);

        cartService.clearCart();

        verify(cartRepository).save(testCart);
        assertThat(testCart.getItems()).isEmpty();
    }
}
