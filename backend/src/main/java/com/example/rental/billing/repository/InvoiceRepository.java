package com.example.rental.billing.repository;

import com.example.rental.billing.entity.Invoice;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByContractRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<Invoice> findDistinctByContractTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userAccountId);

    Optional<Invoice> findByIdAndContractRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select invoice
        from Invoice invoice
        where invoice.id = :id
          and invoice.contract.room.property.landlord.id = :landlordId
          and invoice.deletedAt is null
        """)
    Optional<Invoice> findOwnedByIdForUpdate(@Param("id") Long id, @Param("landlordId") Long landlordId);

    @Query("""
        select case when count(invoice) > 0 then true else false end
        from Invoice invoice
        where invoice.contract.id = :contractId
          and invoice.billingYear = :year
          and invoice.billingMonth = :month
          and invoice.status <> com.example.rental.billing.entity.InvoiceStatus.CANCELLED
          and invoice.deletedAt is null
        """)
    boolean existsOpenForContractPeriod(
        @Param("contractId") Long contractId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );

    @Query("""
        select case when count(invoice) > 0 then true else false end
        from Invoice invoice
        where invoice.contract.room.id = :roomId
          and invoice.billingYear = :year
          and invoice.billingMonth = :month
          and invoice.status <> com.example.rental.billing.entity.InvoiceStatus.CANCELLED
          and invoice.deletedAt is null
        """)
    boolean existsOpenForRoomPeriod(
        @Param("roomId") Long roomId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );

    long countByContractRoomPropertyLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndContractRoomPropertyLandlordEmailNotIn(Collection<String> emails);
}
