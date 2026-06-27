package com.example.rental.contract.repository;

import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.entity.RentalContract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
    List<RentalContract> findByRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<RentalContract> findDistinctByTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userAccountId);

    Optional<RentalContract> findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    boolean existsByRoomIdAndStatusAndDeletedAtIsNull(Long roomId, ContractStatus status);
}
