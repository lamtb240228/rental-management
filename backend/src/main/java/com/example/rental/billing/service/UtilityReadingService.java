package com.example.rental.billing.service;

import com.example.rental.billing.dto.UtilityReadingRequest;
import com.example.rental.billing.dto.UtilityReadingResponse;
import com.example.rental.billing.entity.UtilityReading;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.billing.repository.UtilityReadingRepository;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.property.entity.Room;
import com.example.rental.property.service.RoomService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilityReadingService {
    private final UtilityReadingRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final RoomService roomService;
    private final CurrentUserService currentUserService;

    public UtilityReadingService(
        UtilityReadingRepository repository,
        InvoiceRepository invoiceRepository,
        RoomService roomService,
        CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
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
        Room room = roomService.getOwnedRoomForUpdate(roomId);
        validateReadings(request);
        if (repository.existsByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(roomId, request.billingYear(), request.billingMonth())) {
            throw new ConflictException("Utility reading already exists for this room and period");
        }
        if (invoiceRepository.existsOpenForRoomPeriod(roomId, request.billingYear(), request.billingMonth())) {
            throw new BadRequestException("Cannot create a utility reading after an invoice has been issued for its room and period");
        }
        validateContinuity(roomId, request, null);

        UtilityReading reading = new UtilityReading();
        reading.setRoom(room);
        apply(reading, request);
        repository.save(reading);
        return toResponse(reading);
    }

    @Transactional
    public UtilityReadingResponse update(Long id, UtilityReadingRequest request) {
        Long roomId = repository.findOwnedRoomId(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Utility reading not found"));
        roomService.getOwnedRoomForUpdate(roomId);
        UtilityReading reading = repository.findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Utility reading not found"));
        ensureNotInvoiced(reading, reading.getBillingYear(), reading.getBillingMonth());
        if (!reading.getBillingYear().equals(request.billingYear())
            || !reading.getBillingMonth().equals(request.billingMonth())) {
            ensureNotInvoiced(reading, request.billingYear(), request.billingMonth());
        }
        validateReadings(request);
        if (repository.existsByRoomIdAndBillingYearAndBillingMonthAndIdNotAndDeletedAtIsNull(
            reading.getRoom().getId(), request.billingYear(), request.billingMonth(), reading.getId())) {
            throw new ConflictException("Utility reading already exists for this room and period");
        }
        validateContinuity(reading.getRoom().getId(), request, reading.getId());
        apply(reading, request);
        return toResponse(reading);
    }

    private void ensureNotInvoiced(UtilityReading reading, Integer billingYear, Integer billingMonth) {
        if (reading.getInvoice() != null
            || invoiceRepository.existsOpenForRoomPeriod(
                reading.getRoom().getId(), billingYear, billingMonth)) {
            throw new BadRequestException("Cannot update a utility reading after an invoice has been issued for its room and period");
        }
    }

    private void validateReadings(UtilityReadingRequest request) {
        if (request.electricityNewReading().compareTo(request.electricityOldReading()) < 0
            || request.waterNewReading().compareTo(request.waterOldReading()) < 0) {
            throw new BadRequestException("New readings must be greater than or equal to old readings");
        }
    }

    private void validateContinuity(Long roomId, UtilityReadingRequest request, Long excludedId) {
        repository.findPreviousReading(
                roomId, request.billingYear(), request.billingMonth(), excludedId, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .ifPresent(previous -> {
                if (request.electricityOldReading().compareTo(previous.getElectricityNewReading()) != 0
                    || request.waterOldReading().compareTo(previous.getWaterNewReading()) != 0) {
                    throw new BadRequestException("Old utility readings must match the latest previous readings");
                }
            });

        repository.findNextReading(
                roomId, request.billingYear(), request.billingMonth(), excludedId, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .ifPresent(next -> {
                if (request.electricityNewReading().compareTo(next.getElectricityOldReading()) != 0
                    || request.waterNewReading().compareTo(next.getWaterOldReading()) != 0) {
                    throw new BadRequestException("New utility readings must match the earliest following readings");
                }
            });
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
