package com.foodnest.foodnest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodnest.foodnest.config.DataSeeder;
import com.foodnest.foodnest.dto.request.LoginRequest;
import com.foodnest.foodnest.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
                .email("integrationtest@foodnest.com")
                .password("Test@1234")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("integrationtest@foodnest.com"));
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
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/register - Weak password returns 400")
    void register_withWeakPassword_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test2@foodnest.com")
                .password("password") // no digit
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Wrong credentials returns 401")
    void login_withWrongCredentials_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@foodnest.com")
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
