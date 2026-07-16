package com.example.rental.tenant.repository;

import com.example.rental.tenant.entity.Tenant;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    List<Tenant> findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    List<Tenant> findByIdInAndLandlordIdAndDeletedAtIsNull(Collection<Long> ids, Long landlordId);

    Optional<Tenant> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select tenant
        from Tenant tenant
        where tenant.id = :id
          and tenant.landlord.id = :landlordId
          and tenant.deletedAt is null
        """)
    Optional<Tenant> findOwnedByIdForUpdate(@Param("id") Long id, @Param("landlordId") Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select tenant
        from Tenant tenant
        where tenant.id in :ids
          and tenant.landlord.id = :landlordId
          and tenant.deletedAt is null
        order by tenant.id
        """)
    List<Tenant> findAllOwnedByIdForUpdate(
        @Param("ids") Collection<Long> ids,
        @Param("landlordId") Long landlordId
    );

    Optional<Tenant> findByUserAccountIdAndDeletedAtIsNull(Long userAccountId);

    boolean existsByLandlordIdAndIdentityNumberAndDeletedAtIsNull(Long landlordId, String identityNumber);
}
