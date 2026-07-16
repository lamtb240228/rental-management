package com.example.rental.billing.repository;

import com.example.rental.billing.entity.UtilityReading;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {
    List<UtilityReading> findByRoomIdAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(Long roomId);

    List<UtilityReading> findByRoomIdInAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(Collection<Long> roomIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UtilityReading> findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Query("""
        select reading.room.id
        from UtilityReading reading
        where reading.id = :id
          and reading.room.property.landlord.id = :landlordId
          and reading.deletedAt is null
        """)
    Optional<Long> findOwnedRoomId(@Param("id") Long id, @Param("landlordId") Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UtilityReading> findByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(
        Long roomId,
        Integer year,
        Integer month
    );

    Optional<UtilityReading> findByInvoiceIdAndDeletedAtIsNull(Long invoiceId);

    @Query("""
        select reading
        from UtilityReading reading
        where reading.room.id = :roomId
          and reading.deletedAt is null
          and (:excludedId is null or reading.id <> :excludedId)
          and (reading.billingYear < :year
            or (reading.billingYear = :year and reading.billingMonth < :month))
        order by reading.billingYear desc, reading.billingMonth desc
        """)
    List<UtilityReading> findPreviousReading(
        @Param("roomId") Long roomId,
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("excludedId") Long excludedId,
        Pageable pageable
    );

    @Query("""
        select reading
        from UtilityReading reading
        where reading.room.id = :roomId
          and reading.deletedAt is null
          and (:excludedId is null or reading.id <> :excludedId)
          and (reading.billingYear > :year
            or (reading.billingYear = :year and reading.billingMonth > :month))
        order by reading.billingYear asc, reading.billingMonth asc
        """)
    List<UtilityReading> findNextReading(
        @Param("roomId") Long roomId,
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("excludedId") Long excludedId,
        Pageable pageable
    );

    boolean existsByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(Long roomId, Integer year, Integer month);

    boolean existsByRoomIdAndBillingYearAndBillingMonthAndIdNotAndDeletedAtIsNull(
        Long roomId,
        Integer year,
        Integer month,
        Long id
    );
}
