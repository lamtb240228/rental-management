package com.example.rental.property.repository;

import com.example.rental.property.entity.Room;
import com.example.rental.property.entity.RoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByPropertyIdAndDeletedAtIsNullOrderByRoomNumber(Long propertyId);

    Optional<Room> findByIdAndPropertyLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    boolean existsByPropertyIdAndRoomNumberIgnoreCaseAndDeletedAtIsNull(Long propertyId, String roomNumber);

    long countByPropertyLandlordIdAndStatusAndDeletedAtIsNull(Long landlordId, RoomStatus status);

    long countByDeletedAtIsNull();
}
