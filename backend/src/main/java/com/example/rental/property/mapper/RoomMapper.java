package com.example.rental.property.mapper;

import com.example.rental.property.dto.RoomRequest;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.property.entity.Room;
import com.example.rental.property.entity.RoomStatus;
import java.math.BigDecimal;

public final class RoomMapper {
    private RoomMapper() {
    }

    public static void apply(Room room, RoomRequest request) {
        room.setRoomNumber(request.roomNumber().trim());
        room.setFloorNumber(request.floorNumber());
        room.setArea(request.area());
        room.setMonthlyRent(request.monthlyRent());
        room.setDefaultDeposit(request.defaultDeposit() == null ? BigDecimal.ZERO : request.defaultDeposit());
        room.setMaxOccupants(request.maxOccupants());
        room.setStatus(request.status() == null ? RoomStatus.AVAILABLE : request.status());
        room.setDescription(request.description());
    }

    public static RoomResponse toResponse(Room room) {
        return new RoomResponse(
            room.getId(),
            room.getProperty().getId(),
            room.getProperty().getName(),
            room.getRoomNumber(),
            room.getFloorNumber(),
            room.getArea(),
            room.getMonthlyRent(),
            room.getDefaultDeposit(),
            room.getMaxOccupants(),
            room.getStatus(),
            room.getDescription(),
            room.getCreatedAt(),
            room.getUpdatedAt()
        );
    }
}
