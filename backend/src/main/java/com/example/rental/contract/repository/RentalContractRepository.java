package com.example.rental.contract.repository;

import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.entity.RentalContract;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
    List<RentalContract> findByRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<RentalContract> findDistinctByTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userAccountId);

    List<RentalContract> findDistinctByTenantsTenantIdAndRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        Long tenantId,
        Long landlordId
    );

    Optional<RentalContract> findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select contract
        from RentalContract contract
        where contract.id = :id
          and contract.room.property.landlord.id = :landlordId
          and contract.deletedAt is null
        """)
    Optional<RentalContract> findOwnedByIdForUpdate(@Param("id") Long id, @Param("landlordId") Long landlordId);

    boolean existsByRoomIdAndStatusAndDeletedAtIsNull(Long roomId, ContractStatus status);

    boolean existsByRoomPropertyIdAndStatusAndDeletedAtIsNull(Long propertyId, ContractStatus status);

    boolean existsDistinctByTenantsTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, ContractStatus status);

    boolean existsDistinctByRoomIdAndTenantsTenantIdAndStatusAndDeletedAtIsNull(
        Long roomId,
        Long tenantId,
        ContractStatus status
    );
}
