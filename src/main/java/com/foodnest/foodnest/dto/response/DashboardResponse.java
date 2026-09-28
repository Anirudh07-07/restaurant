package com.foodnest.foodnest.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long totalOrders;
    private long todaysOrders;
    private BigDecimal totalRevenue;
    private long totalCustomers;
    private long pendingOrders;
    private long pendingReservations;
    private double averageRating;
    private long totalReviews;
}
