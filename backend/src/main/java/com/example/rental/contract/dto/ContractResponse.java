package com.example.rental.contract.dto;

import com.example.rental.contract.entity.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ContractResponse(
    Long id,
    Long roomId,
    String roomNumber,
    String contractCode,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal monthlyRent,
    BigDecimal depositAmount,
    ContractStatus status,
    List<ContractTenantResponse> tenants,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
