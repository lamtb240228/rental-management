package com.example.rental.billing.dto;

import java.math.BigDecimal;

public record UtilityReadingResponse(
    Long id,
    Long roomId,
    Integer billingYear,
    Integer billingMonth,
    BigDecimal electricityOldReading,
    BigDecimal electricityNewReading,
    BigDecimal electricityUsage,
    BigDecimal electricityUnitPrice,
    BigDecimal waterOldReading,
    BigDecimal waterNewReading,
    BigDecimal waterUsage,
    BigDecimal waterUnitPrice
) {
}
