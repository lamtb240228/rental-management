package com.example.rental.payment.dto;

import com.example.rental.payment.entity.PaymentMethod;
import com.example.rental.payment.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    OffsetDateTime paidAt,
    @NotNull PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    String transactionReference,
    String note
) {
}
