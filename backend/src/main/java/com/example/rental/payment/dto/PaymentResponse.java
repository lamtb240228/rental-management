package com.example.rental.payment.dto;

import com.example.rental.payment.entity.PaymentMethod;
import com.example.rental.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
    Long id,
    Long invoiceId,
    BigDecimal amount,
    OffsetDateTime paidAt,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    String transactionReference,
    String note,
    Long receivedBy
) {
}
