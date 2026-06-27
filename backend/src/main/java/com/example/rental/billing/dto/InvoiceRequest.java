package com.example.rental.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
    @NotNull Long contractId,
    String invoiceNumber,
    @NotNull @Min(2000) Integer billingYear,
    @NotNull @Min(1) @Max(12) Integer billingMonth,
    @NotNull LocalDate dueDate,
    @DecimalMin(value = "0.00") BigDecimal discountAmount,
    String notes,
    @NotEmpty List<@Valid InvoiceItemRequest> items
) {
}
