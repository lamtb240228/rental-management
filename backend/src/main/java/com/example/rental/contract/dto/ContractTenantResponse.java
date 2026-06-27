package com.example.rental.contract.dto;

public record ContractTenantResponse(
    Long tenantId,
    String fullName,
    boolean primaryTenant
) {
}
