package com.example.rental.billing.dto;

import com.example.rental.billing.entity.InvoiceItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InvoiceItemRequest(
    @NotNull InvoiceItemType itemType,
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice
) {
}
