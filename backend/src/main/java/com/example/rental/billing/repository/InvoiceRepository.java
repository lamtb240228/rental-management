package com.example.rental.billing.repository;

import com.example.rental.billing.entity.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByContractRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<Invoice> findDistinctByContractTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userAccountId);

    Optional<Invoice> findByIdAndContractRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    boolean existsByContractIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(Long contractId, Integer year, Integer month);

    long countByContractRoomPropertyLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByDeletedAtIsNull();
}
