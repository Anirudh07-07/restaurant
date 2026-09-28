package com.kaveri.service;

import com.kaveri.dto.request.ReservationRequest;
import com.kaveri.dto.response.ReservationResponse;
import com.kaveri.enums.ReservationStatus;
import org.springframework.data.domain.Page;

public interface ReservationService {

    ReservationResponse createReservation(ReservationRequest request);

    Page<ReservationResponse> getMyReservations(int page, int size);

    ReservationResponse cancelMyReservation(Long id);

    Page<ReservationResponse> getAllReservations(int page, int size);

    ReservationResponse updateReservationStatus(Long id, ReservationStatus status);
}
