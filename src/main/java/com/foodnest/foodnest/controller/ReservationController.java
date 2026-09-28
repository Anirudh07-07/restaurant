package com.foodnest.foodnest.controller;

import com.foodnest.foodnest.dto.request.ReservationRequest;
import com.foodnest.foodnest.dto.response.ApiResponse;
import com.foodnest.foodnest.dto.response.ReservationResponse;
import com.foodnest.foodnest.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Reservations", description = "Table reservation management")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Make a table reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reservationService.createReservation(request),
                        "Reservation submitted successfully"));
    }

    @GetMapping
    @Operation(summary = "Get my reservations")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.getMyReservations(page, size)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.cancelMyReservation(id), "Reservation cancelled"));
    }
}
