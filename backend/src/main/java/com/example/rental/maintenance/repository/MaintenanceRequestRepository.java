package com.example.rental.maintenance.repository;

import com.example.rental.maintenance.entity.MaintenanceRequest;
import com.example.rental.maintenance.entity.MaintenanceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByRoomPropertyLandlordIdAndDeletedAtIsNullOrderBySubmittedAtDesc(Long landlordId);

    List<MaintenanceRequest> findByTenantUserAccountIdAndDeletedAtIsNullOrderBySubmittedAtDesc(Long userAccountId);

    Optional<MaintenanceRequest> findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    long countByRoomPropertyLandlordIdAndStatusAndDeletedAtIsNull(Long landlordId, MaintenanceStatus status);

    long countByStatusAndDeletedAtIsNull(MaintenanceStatus status);

    long countByStatusAndDeletedAtIsNullAndRoomPropertyLandlordEmailNotIn(
        MaintenanceStatus status,
        Collection<String> emails
    );
}
