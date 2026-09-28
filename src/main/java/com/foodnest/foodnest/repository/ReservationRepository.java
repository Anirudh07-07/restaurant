package com.foodnest.foodnest.repository;

import com.foodnest.foodnest.entity.Reservation;
import com.foodnest.foodnest.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    Page<Reservation> findByReservationDate(LocalDate date, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.reservationDate = :date " +
           "AND r.reservationTime = :time " +
           "AND r.status NOT IN (:cancelledStatuses)")
    List<Reservation> findConflicting(
        @Param("date") LocalDate date,
        @Param("time") LocalTime time,
        @Param("cancelledStatuses") List<ReservationStatus> cancelledStatuses
    );

    long countByStatus(ReservationStatus status);
}
