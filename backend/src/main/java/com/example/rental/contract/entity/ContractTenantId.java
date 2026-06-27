package com.example.rental.contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContractTenantId implements Serializable {
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "tenant_id")
    private Long tenantId;

    public ContractTenantId() {
    }

    public ContractTenantId(Long contractId, Long tenantId) {
        this.contractId = contractId;
        this.tenantId = tenantId;
    }

    public Long getContractId() {
        return contractId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContractTenantId that)) {
            return false;
        }
        return Objects.equals(contractId, that.contractId) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId, tenantId);
    }
}
