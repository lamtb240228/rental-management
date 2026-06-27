package com.example.rental.contract.service;

import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.contract.dto.ContractRequest;
import com.example.rental.contract.dto.ContractResponse;
import com.example.rental.contract.dto.ContractTenantResponse;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.entity.RentalContract;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.property.entity.Room;
import com.example.rental.property.entity.RoomStatus;
import com.example.rental.property.service.RoomService;
import com.example.rental.tenant.entity.Tenant;
import com.example.rental.tenant.repository.TenantRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {
    private final RentalContractRepository contractRepository;
    private final TenantRepository tenantRepository;
    private final RoomService roomService;
    private final CurrentUserService currentUserService;

    public ContractService(
        RentalContractRepository contractRepository,
        TenantRepository tenantRepository,
        RoomService roomService,
        CurrentUserService currentUserService
    ) {
        this.contractRepository = contractRepository;
        this.tenantRepository = tenantRepository;
        this.roomService = roomService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> listMine() {
        return contractRepository.findByRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(currentUserService.currentUserId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long id) {
        return toResponse(getOwnedContract(id));
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        if (request.endDate() != null && !request.endDate().isAfter(request.startDate())) {
            throw new BadRequestException("Contract end date must be after start date");
        }

        Room room = roomService.getOwnedRoom(request.roomId());
        ContractStatus status = request.status() == null ? ContractStatus.ACTIVE : request.status();
        if (status == ContractStatus.ACTIVE) {
            if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.INACTIVE) {
                throw new BadRequestException("Cannot create active contract for maintenance or inactive room");
            }
            if (contractRepository.existsByRoomIdAndStatusAndDeletedAtIsNull(room.getId(), ContractStatus.ACTIVE)) {
                throw new ConflictException("Room already has an active contract");
            }
        }

        Set<Long> tenantIds = request.tenantIds();
        Long primaryTenantId = request.primaryTenantId() == null ? tenantIds.iterator().next() : request.primaryTenantId();
        if (!tenantIds.contains(primaryTenantId)) {
            throw new BadRequestException("Primary tenant must be included in tenantIds");
        }

        List<Tenant> tenants = tenantRepository.findByIdInAndLandlordIdAndDeletedAtIsNull(tenantIds, currentUserService.currentUserId());
        if (tenants.size() != tenantIds.size()) {
            throw new BadRequestException("One or more tenants are invalid");
        }

        RentalContract contract = new RentalContract();
        contract.setRoom(room);
        contract.setContractCode(resolveContractCode(request.contractCode()));
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setMonthlyRent(request.monthlyRent());
        contract.setDepositAmount(request.depositAmount());
        contract.setStatus(status);
        contract.setNotes(request.notes());
        tenants.forEach(tenant -> contract.addTenant(tenant, tenant.getId().equals(primaryTenantId)));

        if (status == ContractStatus.ACTIVE) {
            room.setStatus(RoomStatus.OCCUPIED);
        }

        contractRepository.save(contract);
        return toResponse(contract);
    }

    @Transactional
    public ContractResponse end(Long id, LocalDate endDate) {
        RentalContract contract = getOwnedContract(id);
        contract.setStatus(ContractStatus.ENDED);
        contract.setEndDate(endDate == null ? LocalDate.now() : endDate);
        contract.getRoom().setStatus(RoomStatus.AVAILABLE);
        return toResponse(contract);
    }

    public RentalContract getOwnedContract(Long id) {
        return contractRepository.findByIdAndRoomPropertyLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Contract not found"));
    }

    private String resolveContractCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.trim();
        }
        return "CT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.nanoTime();
    }

    public ContractResponse toResponse(RentalContract contract) {
        List<ContractTenantResponse> tenants = contract.getTenants().stream()
            .sorted(Comparator.comparing(contractTenant -> contractTenant.getTenant().getFullName()))
            .map(contractTenant -> new ContractTenantResponse(
                contractTenant.getTenant().getId(),
                contractTenant.getTenant().getFullName(),
                contractTenant.isPrimaryTenant()
            ))
            .toList();

        return new ContractResponse(
            contract.getId(),
            contract.getRoom().getId(),
            contract.getRoom().getRoomNumber(),
            contract.getContractCode(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getMonthlyRent(),
            contract.getDepositAmount(),
            contract.getStatus(),
            tenants,
            contract.getNotes(),
            contract.getCreatedAt(),
            contract.getUpdatedAt()
        );
    }
}
