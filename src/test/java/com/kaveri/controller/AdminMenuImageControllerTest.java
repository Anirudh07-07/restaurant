package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.LoginRequest;
import com.kaveri.entity.FoodItem;
import com.kaveri.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin Menu Image Security & Management Integration Tests")
class AdminMenuImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodItemRepository foodItemRepository;

    private String adminToken;
    private String customerToken;
    private Long testFoodId;

    @BeforeEach
    void setUp() throws Exception {
        // Obtain Admin Token
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@thekaveri.com")
                .password("Admin@1234")
                .build();

        MvcResult adminRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminJson = adminRes.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminJson).get("data").get("token").asText();

        // Obtain Customer Token
        LoginRequest custLogin = LoginRequest.builder()
                .email("customer@thekaveri.com")
                .password("Customer@1234")
                .build();

        MvcResult custRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(custLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String custJson = custRes.getResponse().getContentAsString();
        customerToken = objectMapper.readTree(custJson).get("data").get("token").asText();

        // Get an existing food item
        FoodItem food = foodItemRepository.findAll().stream().findFirst().orElseThrow();
        testFoodId = food.getId();
    }

    @Test
    @DisplayName("Upload image without auth is rejected (403 Forbidden)")
    void uploadImage_withoutAuth_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/menu/{id}/image", testFoodId)
                .file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Upload image with CUSTOMER role is rejected with 403 Forbidden")
    void uploadImage_withCustomerRole_rejectedWith403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/menu/{id}/image", testFoodId)
                .file(file)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Upload valid JPEG image with ADMIN role succeeds with 200")
    void uploadImage_withAdminRole_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "paneer-tikka.jpg", "image/jpeg", "sample-valid-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/menu/{id}/image", testFoodId)
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageUrl").value(startsWith("/uploads/")));

        FoodItem updated = foodItemRepository.findById(testFoodId).orElseThrow();
        assertNotNull(updated.getImageUrl());
        assertTrue(updated.getImageUrl().startsWith("/uploads/"));
    }

    @Test
    @DisplayName("Remove image without auth is rejected (403 Forbidden)")
    void removeImage_withoutAuth_rejected() throws Exception {
        mockMvc.perform(delete("/api/admin/menu/{id}/image", testFoodId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Remove image with CUSTOMER role is rejected with 403 Forbidden")
    void removeImage_withCustomerRole_rejectedWith403() throws Exception {
        mockMvc.perform(delete("/api/admin/menu/{id}/image", testFoodId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Remove image with ADMIN role succeeds with 200 and clears imageUrl")
    void removeImage_withAdminRole_succeeds() throws Exception {
        mockMvc.perform(delete("/api/admin/menu/{id}/image", testFoodId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        FoodItem updated = foodItemRepository.findById(testFoodId).orElseThrow();
        assertNull(updated.getImageUrl());
    }

    @Test
    @DisplayName("Upload invalid file type (e.g. text file) is rejected with 400 Bad Request")
    void uploadImage_invalidFileType_rejectedWith400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "text/plain", "echo malicious".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/menu/{id}/image", testFoodId)
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Upload oversized file (>5MB) is rejected with 400 Bad Request")
    void uploadImage_oversizedFile_rejectedWith400() throws Exception {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.jpg", "image/jpeg", largeBytes
        );

        mockMvc.perform(multipart("/api/admin/menu/{id}/image", testFoodId)
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
