package com.example.rental.property.repository;

import com.example.rental.property.entity.Property;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long landlordId);

    Optional<Property> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    long countByLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByDeletedAtIsNull();
}
