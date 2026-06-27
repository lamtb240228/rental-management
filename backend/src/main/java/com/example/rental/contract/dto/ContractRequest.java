package com.example.rental.contract.dto;

import com.example.rental.contract.entity.ContractStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record ContractRequest(
    @NotNull Long roomId,
    String contractCode,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotNull @DecimalMin(value = "0.00") BigDecimal monthlyRent,
    @NotNull @DecimalMin(value = "0.00") BigDecimal depositAmount,
    ContractStatus status,
    @NotEmpty Set<Long> tenantIds,
    Long primaryTenantId,
    String notes
) {
}
