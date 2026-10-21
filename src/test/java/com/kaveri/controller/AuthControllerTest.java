package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.LoginRequest;
import com.kaveri.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register - Valid request returns 201 with token")
    void register_withValidRequest_returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Integration Test User")
                .email("integrationtest@thekaveri.com")
                .password("Test@1234")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("integrationtest@thekaveri.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - With +91 formatted phone returns 201")
    void register_withPlus91Phone_returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Plus91 User")
                .email("plus91@thekaveri.com")
                .password("Test@1234")
                .phone("+91 9876543210")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/register - With 0-prefixed phone returns 201")
    void register_withLeadingZeroPhone_returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Zero Prefix User")
                .email("zero@thekaveri.com")
                .password("Test@1234")
                .phone("09876543210")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/register - Empty phone string returns 201 (optional phone)")
    void register_withEmptyPhone_returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Empty Phone User")
                .email("emptyphone@thekaveri.com")
                .password("Test@1234")
                .phone("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid phone returns 400 with specific field error")
    void register_withInvalidPhone_returns400WithSpecificMessage() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Invalid Phone User")
                .email("invalidphone@thekaveri.com")
                .password("Test@1234")
                .phone("12345")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").isNotEmpty())
                .andExpect(jsonPath("$.message", containsString("phone")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid email returns 400")
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test")
                .email("not-an-email")
                .password("Test@1234")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").isNotEmpty())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Weak password returns 400 with specific message")
    void register_withWeakPassword_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test2@thekaveri.com")
                .password("password") // no digit
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").isNotEmpty())
                .andExpect(jsonPath("$.message", containsString("Password")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Wrong credentials returns 401")
    void login_withWrongCredentials_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@thekaveri.com")
                .password("WrongPassword1")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - Unauthenticated returns 403")
    void adminDashboard_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
