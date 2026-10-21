package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.ReviewRequest;
import com.kaveri.dto.response.ReviewResponse;
import com.kaveri.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ReviewController Integration Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Test
    @WithMockUser(username = "customer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/reviews/restaurant - Submits valid restaurant review")
    void createRestaurantReview_validRequest_returns201() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("[Kaveri Restaurant] Incredible royal thali and hospitality.")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .userName("Rahul Sharma")
                .rating(5)
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(ReviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reviews/restaurant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value(request.getComment()));
    }

    @Test
    @DisplayName("POST /api/reviews/restaurant - Unauthenticated request returns 403 Forbidden")
    void createRestaurantReview_unauthenticated_returnsUnauthorized() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Anonymous review")
                .build();

        mockMvc.perform(post("/api/reviews/restaurant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
