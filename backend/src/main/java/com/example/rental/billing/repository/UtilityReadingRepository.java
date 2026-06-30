package com.example.rental.billing.repository;

import com.example.rental.billing.entity.UtilityReading;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {
    List<UtilityReading> findByRoomIdAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(Long roomId);

    List<UtilityReading> findByRoomIdInAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(Collection<Long> roomIds);

    Optional<UtilityReading> findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    boolean existsByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(Long roomId, Integer year, Integer month);

    boolean existsByRoomIdAndBillingYearAndBillingMonthAndIdNotAndDeletedAtIsNull(
        Long roomId,
        Integer year,
        Integer month,
        Long id
    );
}
