package com.example.rental.contract.entity;

import com.example.rental.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contract_tenants")
public class ContractTenant {
    @EmbeddedId
    private ContractTenantId id = new ContractTenantId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("contractId")
    @JoinColumn(name = "contract_id")
    private RentalContract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tenantId")
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "primary_tenant", nullable = false)
    private boolean primaryTenant;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ContractTenant() {
    }

    public ContractTenant(RentalContract contract, Tenant tenant, boolean primaryTenant, LocalDate moveInDate) {
        this.contract = contract;
        this.tenant = tenant;
        this.primaryTenant = primaryTenant;
        this.moveInDate = moveInDate;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public boolean isPrimaryTenant() {
        return primaryTenant;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public LocalDate getMoveOutDate() {
        return moveOutDate;
    }
}
