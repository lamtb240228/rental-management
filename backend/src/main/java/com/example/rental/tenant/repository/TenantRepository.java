package com.example.rental.tenant.repository;

import com.example.rental.tenant.entity.Tenant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    List<Tenant> findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<Tenant> findByIdInAndLandlordIdAndDeletedAtIsNull(Collection<Long> ids, Long landlordId);

    Optional<Tenant> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    Optional<Tenant> findByUserAccountIdAndDeletedAtIsNull(Long userAccountId);

    boolean existsByLandlordIdAndIdentityNumberAndDeletedAtIsNull(Long landlordId, String identityNumber);
}
