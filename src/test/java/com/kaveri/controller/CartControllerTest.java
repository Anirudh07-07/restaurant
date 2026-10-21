package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.CartItemRequest;
import com.kaveri.entity.Category;
import com.kaveri.entity.FoodItem;
import com.kaveri.entity.Role;
import com.kaveri.entity.User;
import com.kaveri.enums.RoleName;
import com.kaveri.repository.CategoryRepository;
import com.kaveri.repository.FoodItemRepository;
import com.kaveri.repository.RoleRepository;
import com.kaveri.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CartController Integration Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.kaveri.repository.CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private FoodItem sampleFood;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_CUSTOMER).build()));

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build()));

        if (!userRepository.existsByEmail("cartcustomer@thekaveri.com")) {
            userRepository.save(User.builder()
                    .name("Cart Customer")
                    .email("cartcustomer@thekaveri.com")
                    .password(passwordEncoder.encode("Test@1234"))
                    .roles(Set.of(customerRole))
                    .enabled(true)
                    .build());
        }

        if (!userRepository.existsByEmail("cartadmin@thekaveri.com")) {
            userRepository.save(User.builder()
                    .name("Cart Admin")
                    .email("cartadmin@thekaveri.com")
                    .password(passwordEncoder.encode("Test@1234"))
                    .roles(Set.of(adminRole))
                    .enabled(true)
                    .build());
        }

        Category category = categoryRepository.findByNameIgnoreCase("Beverages").orElseGet(() ->
                categoryRepository.save(Category.builder().name("Beverages").build()));

        sampleFood = foodItemRepository.findByNameIgnoreCase("Virgin Mojito Cart Test").orElseGet(() ->
                foodItemRepository.save(FoodItem.builder()
                        .name("Virgin Mojito Cart Test")
                        .price(new BigDecimal("120.00"))
                        .category(category)
                        .available(true)
                        .vegetarian(true)
                        .build()));
    }

    @Test
    @DisplayName("GET /api/cart - Unauthenticated request returns 401/403")
    void getCart_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cartcustomer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/cart - New user without existing cart returns 200 with empty cart")
    void getCart_newUserWithoutCart_returns200EmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemCount").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.subtotal").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.deliveryFee").value(0));
    }

    @Test
    @WithMockUser(username = "cartadmin@thekaveri.com", roles = {"ADMIN"})
    @DisplayName("GET /api/cart - Admin user can also access cart")
    void getCart_adminUser_returns200() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "cartcustomer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/cart/items - Add by foodItemId succeeds and updates cart")
    void addItem_byFoodItemId_succeeds() throws Exception {
        CartItemRequest request = CartItemRequest.builder()
                .foodItemId(sampleFood.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.subtotal", greaterThan(0.0)));
    }

    @Test
    @WithMockUser(username = "cartcustomer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/cart/items - Add by foodName succeeds")
    void addItem_byFoodName_succeeds() throws Exception {
        CartItemRequest request = CartItemRequest.builder()
                .foodName("Virgin Mojito Cart Test")
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "cartcustomer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("DELETE /api/cart - Clears all items from cart")
    void clearCart_succeeds() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }
}
