package com.foodnest.foodnest.service.impl;

import com.foodnest.foodnest.dto.request.ReservationRequest;
import com.foodnest.foodnest.dto.response.ReservationResponse;
import com.foodnest.foodnest.entity.Reservation;
import com.foodnest.foodnest.entity.User;
import com.foodnest.foodnest.enums.ReservationStatus;
import com.foodnest.foodnest.exception.BadRequestException;
import com.foodnest.foodnest.exception.ResourceNotFoundException;
import com.foodnest.foodnest.exception.UnauthorizedException;
import com.foodnest.foodnest.repository.ReservationRepository;
import com.foodnest.foodnest.service.NotificationService;
import com.foodnest.foodnest.service.ReservationService;
import com.foodnest.foodnest.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = securityUtils.getCurrentUser();

        // Check for conflicting reservations
        List<Reservation> conflicts = reservationRepository.findConflicting(
                request.getReservationDate(),
                request.getReservationTime(),
                List.of(ReservationStatus.CANCELLED, ReservationStatus.REJECTED)
        );

        // Allow up to 10 simultaneous reservations for same slot
        if (conflicts.size() >= 10) {
            throw new BadRequestException(
                "No tables available for " + request.getReservationDate() +
                " at " + request.getReservationTime() + ". Please choose a different time.");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .numberOfGuests(request.getNumberOfGuests())
                .specialRequest(request.getSpecialRequest())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        notificationService.createNotification(user,
                "Reservation Requested",
                "Your reservation for " + request.getReservationDate() +
                " at " + request.getReservationTime() + " is pending confirmation.");

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(int page, int size) {
        User user = securityUtils.getCurrentUser();
        return reservationRepository.findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by("reservationDate").descending()))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ReservationResponse cancelMyReservation(Long id) {
        User user = securityUtils.getCurrentUser();
        Reservation reservation = reservationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (reservation.getStatus() == ReservationStatus.COMPLETED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("This reservation cannot be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(int page, int size) {
        return reservationRepository
                .findAll(PageRequest.of(page, size, Sort.by("reservationDate").descending()))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        reservation.setStatus(status);
        Reservation saved = reservationRepository.save(reservation);

        String message = switch (status) {
            case CONFIRMED -> "Your reservation on " + reservation.getReservationDate() + " has been confirmed! 🎉";
            case REJECTED -> "Unfortunately, your reservation on " + reservation.getReservationDate() + " was rejected.";
            case COMPLETED -> "Your reservation has been marked as completed. Thank you for dining with us!";
            default -> "Your reservation status has been updated to: " + status;
        };

        notificationService.createNotification(reservation.getUser(), "Reservation Update", message);

        return toResponse(saved);
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .userEmail(r.getUser().getEmail())
                .reservationDate(r.getReservationDate())
                .reservationTime(r.getReservationTime())
                .numberOfGuests(r.getNumberOfGuests())
                .status(r.getStatus())
                .specialRequest(r.getSpecialRequest())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
