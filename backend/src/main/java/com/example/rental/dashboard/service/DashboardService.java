package com.example.rental.dashboard.service;

import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.dashboard.dto.DashboardSummaryResponse;
import com.example.rental.maintenance.entity.MaintenanceStatus;
import com.example.rental.maintenance.repository.MaintenanceRequestRepository;
import com.example.rental.property.entity.RoomStatus;
import com.example.rental.property.repository.PropertyRepository;
import com.example.rental.property.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CurrentUserService currentUserService;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceRequestRepository maintenanceRepository;

    public DashboardService(
        CurrentUserService currentUserService,
        PropertyRepository propertyRepository,
        RoomRepository roomRepository,
        InvoiceRepository invoiceRepository,
        MaintenanceRequestRepository maintenanceRepository
    ) {
        this.currentUserService = currentUserService;
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        Long landlordId = currentUserService.currentUserId();
        return new DashboardSummaryResponse(
            propertyRepository.countByLandlordIdAndDeletedAtIsNull(landlordId),
            roomRepository.countByPropertyLandlordIdAndStatusAndDeletedAtIsNull(landlordId, RoomStatus.AVAILABLE),
            roomRepository.countByPropertyLandlordIdAndStatusAndDeletedAtIsNull(landlordId, RoomStatus.OCCUPIED),
            invoiceRepository.countByContractRoomPropertyLandlordIdAndDeletedAtIsNull(landlordId),
            maintenanceRepository.countByRoomPropertyLandlordIdAndStatusAndDeletedAtIsNull(landlordId, MaintenanceStatus.PENDING)
        );
    }
}
