package com.example.rental.property.repository;

import com.example.rental.property.entity.Property;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    Optional<Property> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select property
        from Property property
        where property.id = :id
          and property.landlord.id = :landlordId
          and property.deletedAt is null
        """)
    Optional<Property> findOwnedByIdForUpdate(@Param("id") Long id, @Param("landlordId") Long landlordId);

    long countByLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndLandlordEmailNotIn(Collection<String> emails);
}
