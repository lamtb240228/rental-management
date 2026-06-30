package com.example.rental.billing.service;

import com.example.rental.billing.dto.UtilityReadingRequest;
import com.example.rental.billing.dto.UtilityReadingResponse;
import com.example.rental.billing.entity.UtilityReading;
import com.example.rental.billing.repository.UtilityReadingRepository;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.property.entity.Room;
import com.example.rental.property.service.RoomService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilityReadingService {
    private final UtilityReadingRepository repository;
    private final RoomService roomService;
    private final CurrentUserService currentUserService;

    public UtilityReadingService(UtilityReadingRepository repository, RoomService roomService, CurrentUserService currentUserService) {
        this.repository = repository;
        this.roomService = roomService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingResponse> listByRoom(Long roomId) {
        roomService.getOwnedRoom(roomId);
        return repository.findByRoomIdAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(roomId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public UtilityReadingResponse create(Long roomId, UtilityReadingRequest request) {
        Room room = roomService.getOwnedRoom(roomId);
        validateReadings(request);
        if (repository.existsByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(roomId, request.billingYear(), request.billingMonth())) {
            throw new ConflictException("Utility reading already exists for this room and period");
        }

        UtilityReading reading = new UtilityReading();
        reading.setRoom(room);
        apply(reading, request);
        repository.save(reading);
        return toResponse(reading);
    }

    @Transactional
    public UtilityReadingResponse update(Long id, UtilityReadingRequest request) {
        UtilityReading reading = repository.findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Utility reading not found"));
        validateReadings(request);
        if (repository.existsByRoomIdAndBillingYearAndBillingMonthAndIdNotAndDeletedAtIsNull(
            reading.getRoom().getId(), request.billingYear(), request.billingMonth(), reading.getId())) {
            throw new ConflictException("Utility reading already exists for this room and period");
        }
        apply(reading, request);
        return toResponse(reading);
    }

    private void validateReadings(UtilityReadingRequest request) {
        if (request.electricityNewReading().compareTo(request.electricityOldReading()) < 0
            || request.waterNewReading().compareTo(request.waterOldReading()) < 0) {
            throw new BadRequestException("New readings must be greater than or equal to old readings");
        }
    }

    private void apply(UtilityReading reading, UtilityReadingRequest request) {
        reading.setBillingYear(request.billingYear());
        reading.setBillingMonth(request.billingMonth());
        reading.setElectricityOldReading(request.electricityOldReading());
        reading.setElectricityNewReading(request.electricityNewReading());
        reading.setElectricityUnitPrice(request.electricityUnitPrice());
        reading.setWaterOldReading(request.waterOldReading());
        reading.setWaterNewReading(request.waterNewReading());
        reading.setWaterUnitPrice(request.waterUnitPrice());
    }

    public UtilityReadingResponse toResponse(UtilityReading reading) {
        return new UtilityReadingResponse(
            reading.getId(),
            reading.getRoom().getId(),
            reading.getBillingYear(),
            reading.getBillingMonth(),
            reading.getElectricityOldReading(),
            reading.getElectricityNewReading(),
            reading.getElectricityUsage(),
            reading.getElectricityUnitPrice(),
            reading.getWaterOldReading(),
            reading.getWaterNewReading(),
            reading.getWaterUsage(),
            reading.getWaterUnitPrice()
        );
    }
}
