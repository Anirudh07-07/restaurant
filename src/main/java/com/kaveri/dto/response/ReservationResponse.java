package com.kaveri.dto.response;

import com.kaveri.enums.ReservationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private int numberOfGuests;
    private ReservationStatus status;
    private String specialRequest;
    private LocalDateTime createdAt;
}
