package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.ReservationRequest;
import com.kaveri.dto.response.ReservationResponse;
import com.kaveri.enums.ReservationStatus;
import com.kaveri.service.ReservationService;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ReservationController Integration Tests")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @Test
    @WithMockUser(username = "customer@thekaveri.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/reservations - Valid reservation returns 201")
    void createReservation_validRequest_returns201() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .reservationDate(LocalDate.now().plusDays(1))
                .reservationTime(LocalTime.of(19, 30))
                .numberOfGuests(4)
                .specialRequest("Window table please")
                .build();

        ReservationResponse response = ReservationResponse.builder()
                .id(10L)
                .userName("Rahul Sharma")
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .numberOfGuests(4)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationService.createReservation(any(ReservationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.numberOfGuests").value(4))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/reservations - Anonymous user returns 403 Forbidden")
    void createReservation_unauthenticated_returns401() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .reservationDate(LocalDate.now().plusDays(1))
                .reservationTime(LocalTime.of(19, 30))
                .numberOfGuests(4)
                .build();

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
