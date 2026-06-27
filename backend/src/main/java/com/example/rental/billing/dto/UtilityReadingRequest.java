package com.example.rental.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UtilityReadingRequest(
    @NotNull @Min(2000) Integer billingYear,
    @NotNull @Min(1) @Max(12) Integer billingMonth,
    @NotNull @DecimalMin(value = "0.00") BigDecimal electricityOldReading,
    @NotNull @DecimalMin(value = "0.00") BigDecimal electricityNewReading,
    @NotNull @DecimalMin(value = "0.00") BigDecimal electricityUnitPrice,
    @NotNull @DecimalMin(value = "0.00") BigDecimal waterOldReading,
    @NotNull @DecimalMin(value = "0.00") BigDecimal waterNewReading,
    @NotNull @DecimalMin(value = "0.00") BigDecimal waterUnitPrice
) {
}
