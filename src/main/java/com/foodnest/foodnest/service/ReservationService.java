package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.ReservationRequest;
import com.foodnest.foodnest.dto.response.ReservationResponse;
import com.foodnest.foodnest.enums.ReservationStatus;
import org.springframework.data.domain.Page;

public interface ReservationService {

    ReservationResponse createReservation(ReservationRequest request);

    Page<ReservationResponse> getMyReservations(int page, int size);

    ReservationResponse cancelMyReservation(Long id);

    Page<ReservationResponse> getAllReservations(int page, int size);

    ReservationResponse updateReservationStatus(Long id, ReservationStatus status);
}
