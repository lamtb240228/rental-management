package com.example.rental.billing.dto;

import com.example.rental.billing.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    Long contractId,
    String invoiceNumber,
    Integer billingYear,
    Integer billingMonth,
    LocalDate issueDate,
    LocalDate dueDate,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    InvoiceStatus status,
    String notes,
    List<InvoiceItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
