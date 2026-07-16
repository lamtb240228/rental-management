package com.example.rental.property.repository;

import com.example.rental.property.entity.Room;
import com.example.rental.property.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByPropertyIdAndDeletedAtIsNullOrderByRoomNumber(Long propertyId);

    Optional<Room> findByIdAndPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select room
        from Room room
        where room.id = :id
          and room.property.landlord.id = :landlordId
          and room.deletedAt is null
        """)
    Optional<Room> findOwnedByIdForUpdate(@Param("id") Long id, @Param("landlordId") Long landlordId);

    boolean existsByPropertyIdAndRoomNumberIgnoreCaseAndDeletedAtIsNull(Long propertyId, String roomNumber);

    long countByPropertyLandlordIdAndStatusAndDeletedAtIsNull(Long landlordId, RoomStatus status);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndPropertyLandlordEmailNotIn(Collection<String> emails);
}
