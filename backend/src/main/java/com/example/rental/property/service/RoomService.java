package com.example.rental.property.service;

import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.property.dto.RoomRequest;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.property.entity.Property;
import com.example.rental.property.entity.Room;
import com.example.rental.property.mapper.RoomMapper;
import com.example.rental.property.repository.RoomRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final PropertyService propertyService;
    private final CurrentUserService currentUserService;
    private final RentalContractRepository contractRepository;

    public RoomService(
        RoomRepository roomRepository,
        PropertyService propertyService,
        CurrentUserService currentUserService,
        RentalContractRepository contractRepository
    ) {
        this.roomRepository = roomRepository;
        this.propertyService = propertyService;
        this.currentUserService = currentUserService;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listByProperty(Long propertyId) {
        propertyService.getOwnedProperty(propertyId);
        return roomRepository.findByPropertyIdAndDeletedAtIsNullOrderByRoomNumber(propertyId)
            .stream()
            .map(RoomMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long id) {
        return RoomMapper.toResponse(getOwnedRoom(id));
    }

    @Transactional
    public RoomResponse create(Long propertyId, RoomRequest request) {
        Property property = propertyService.getOwnedProperty(propertyId);
        if (roomRepository.existsByPropertyIdAndRoomNumberIgnoreCaseAndDeletedAtIsNull(propertyId, request.roomNumber())) {
            throw new ConflictException("Room number already exists in this property");
        }

        Room room = new Room();
        room.setProperty(property);
        RoomMapper.apply(room, request);
        roomRepository.save(room);
        return RoomMapper.toResponse(room);
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = getOwnedRoom(id);
        if (!room.getRoomNumber().equalsIgnoreCase(request.roomNumber())
            && roomRepository.existsByPropertyIdAndRoomNumberIgnoreCaseAndDeletedAtIsNull(room.getProperty().getId(), request.roomNumber())) {
            throw new ConflictException("Room number already exists in this property");
        }
        RoomMapper.apply(room, request);
        return RoomMapper.toResponse(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = getOwnedRoom(id);
        if (contractRepository.existsByRoomIdAndStatusAndDeletedAtIsNull(id, ContractStatus.ACTIVE)) {
            throw new BadRequestException("Cannot remove a room with an active contract");
        }
        room.softDelete();
    }

    public Room getOwnedRoom(Long id) {
        Long landlordId = currentUserService.currentUserId();
        return roomRepository.findByIdAndPropertyLandlordIdAndDeletedAtIsNull(id, landlordId)
            .orElseThrow(() -> new NotFoundException("Room not found"));
    }
}
