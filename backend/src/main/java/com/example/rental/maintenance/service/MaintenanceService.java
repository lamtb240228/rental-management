package com.example.rental.maintenance.service;

import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.maintenance.dto.MaintenanceRequestCreateRequest;
import com.example.rental.maintenance.dto.MaintenanceRequestResponse;
import com.example.rental.maintenance.dto.MaintenanceStatusUpdateRequest;
import com.example.rental.maintenance.entity.MaintenancePriority;
import com.example.rental.maintenance.entity.MaintenanceRequest;
import com.example.rental.maintenance.entity.MaintenanceStatus;
import com.example.rental.maintenance.repository.MaintenanceRequestRepository;
import com.example.rental.property.entity.Room;
import com.example.rental.property.service.RoomService;
import com.example.rental.tenant.entity.Tenant;
import com.example.rental.tenant.service.TenantService;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
    private final MaintenanceRequestRepository repository;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final RentalContractRepository contractRepository;

    public MaintenanceService(
        MaintenanceRequestRepository repository,
        RoomService roomService,
        TenantService tenantService,
        UserAccountRepository userAccountRepository,
        CurrentUserService currentUserService,
        RentalContractRepository contractRepository
    ) {
        this.repository = repository;
        this.roomService = roomService;
        this.tenantService = tenantService;
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> listMine() {
        return repository.findByRoomPropertyLandlordIdAndDeletedAtIsNullOrderBySubmittedAtDesc(currentUserService.currentUserId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MaintenanceRequestResponse create(MaintenanceRequestCreateRequest request) {
        Room room = roomService.getOwnedRoom(request.roomId());
        Tenant tenant = request.tenantId() == null ? null : tenantService.getOwnedTenant(request.tenantId());
        if (tenant != null) {
            ensureTenantOccupiesRoom(room, tenant);
        }
        UserAccount user = userAccountRepository.findById(currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Current user not found"));

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();
        maintenanceRequest.setRoom(room);
        maintenanceRequest.setTenant(tenant);
        maintenanceRequest.setCreatedBy(user);
        maintenanceRequest.setTitle(request.title().trim());
        maintenanceRequest.setDescription(request.description().trim());
        maintenanceRequest.setPriority(request.priority() == null ? MaintenancePriority.MEDIUM : request.priority());
        repository.save(maintenanceRequest);
        return toResponse(maintenanceRequest);
    }

    @Transactional
    public MaintenanceRequestResponse createForTenant(
        Room room,
        Tenant tenant,
        String title,
        String description,
        MaintenancePriority priority
    ) {
        ensureTenantOccupiesRoom(room, tenant);
        UserAccount user = userAccountRepository.findById(currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Current user not found"));

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();
        maintenanceRequest.setRoom(room);
        maintenanceRequest.setTenant(tenant);
        maintenanceRequest.setCreatedBy(user);
        maintenanceRequest.setTitle(title.trim());
        maintenanceRequest.setDescription(description.trim());
        maintenanceRequest.setPriority(priority == null ? MaintenancePriority.MEDIUM : priority);
        repository.save(maintenanceRequest);
        return toResponse(maintenanceRequest);
    }

    @Transactional
    public MaintenanceRequestResponse updateStatus(Long id, MaintenanceStatusUpdateRequest request) {
        MaintenanceRequest maintenanceRequest = repository.findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(
            id, currentUserService.currentUserId()).orElseThrow(() -> new NotFoundException("Maintenance request not found"));
        if ((maintenanceRequest.getStatus() == MaintenanceStatus.CANCELLED
            || maintenanceRequest.getStatus() == MaintenanceStatus.COMPLETED)
            && request.status() != maintenanceRequest.getStatus()) {
            throw new BadRequestException("A completed or cancelled request cannot be reopened");
        }
        maintenanceRequest.setStatus(request.status());
        maintenanceRequest.setResolutionNotes(request.resolutionNotes());
        return toResponse(maintenanceRequest);
    }

    private void ensureTenantOccupiesRoom(Room room, Tenant tenant) {
        if (!contractRepository.existsDistinctByRoomIdAndTenantsTenantIdAndStatusAndDeletedAtIsNull(
            room.getId(), tenant.getId(), ContractStatus.ACTIVE)) {
            throw new BadRequestException("Tenant does not have an active contract for this room");
        }
    }

    public MaintenanceRequestResponse toResponse(MaintenanceRequest request) {
        return new MaintenanceRequestResponse(
            request.getId(),
            request.getRoom().getId(),
            request.getRoom().getRoomNumber(),
            request.getTenant() == null ? null : request.getTenant().getId(),
            request.getTenant() == null ? null : request.getTenant().getFullName(),
            request.getCreatedBy().getId(),
            request.getTitle(),
            request.getDescription(),
            request.getPriority(),
            request.getStatus(),
            request.getSubmittedAt(),
            request.getStartedAt(),
            request.getCompletedAt(),
            request.getCancelledAt(),
            request.getResolutionNotes()
        );
    }
}
