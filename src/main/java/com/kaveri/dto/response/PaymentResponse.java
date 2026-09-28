package com.kaveri.dto.response;

import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String razorpayOrderId;
    private String transactionId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;

    /** Returned to frontend so Razorpay checkout can be opened */
    private String razorpayKeyId;
}
