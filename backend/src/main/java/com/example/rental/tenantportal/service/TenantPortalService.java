package com.example.rental.tenantportal.service;

import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.dto.UtilityReadingResponse;
import com.example.rental.billing.entity.UtilityReading;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.billing.repository.UtilityReadingRepository;
import com.example.rental.billing.service.InvoiceService;
import com.example.rental.billing.service.UtilityReadingService;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.contract.dto.ContractResponse;
import com.example.rental.contract.entity.ContractTenant;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.entity.RentalContract;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.contract.service.ContractService;
import com.example.rental.maintenance.dto.MaintenanceRequestResponse;
import com.example.rental.maintenance.repository.MaintenanceRequestRepository;
import com.example.rental.maintenance.service.MaintenanceService;
import com.example.rental.payment.dto.PaymentResponse;
import com.example.rental.payment.repository.PaymentRepository;
import com.example.rental.payment.service.PaymentService;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.property.entity.Room;
import com.example.rental.property.mapper.RoomMapper;
import com.example.rental.tenant.dto.TenantResponse;
import com.example.rental.tenant.entity.Tenant;
import com.example.rental.tenant.service.TenantService;
import com.example.rental.tenantportal.dto.TenantMaintenanceCreateRequest;
import com.example.rental.tenantportal.dto.TenantPortalSummaryResponse;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantPortalService {
    private final CurrentUserService currentUserService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final UtilityReadingService utilityReadingService;
    private final PaymentService paymentService;
    private final MaintenanceService maintenanceService;
    private final RentalContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceRequestRepository maintenanceRepository;

    public TenantPortalService(
        CurrentUserService currentUserService,
        TenantService tenantService,
        ContractService contractService,
        InvoiceService invoiceService,
        UtilityReadingService utilityReadingService,
        PaymentService paymentService,
        MaintenanceService maintenanceService,
        RentalContractRepository contractRepository,
        InvoiceRepository invoiceRepository,
        UtilityReadingRepository utilityReadingRepository,
        PaymentRepository paymentRepository,
        MaintenanceRequestRepository maintenanceRepository
    ) {
        this.currentUserService = currentUserService;
        this.tenantService = tenantService;
        this.contractService = contractService;
        this.invoiceService = invoiceService;
        this.utilityReadingService = utilityReadingService;
        this.paymentService = paymentService;
        this.maintenanceService = maintenanceService;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.utilityReadingRepository = utilityReadingRepository;
        this.paymentRepository = paymentRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional(readOnly = true)
    public TenantPortalSummaryResponse summary() {
        Long userAccountId = currentUserService.currentUserId();
        Tenant tenant = tenantService.getCurrentTenant();
        List<RentalContract> contracts = contractRepository
            .findDistinctByTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(userAccountId);
        RentalContract activeContract = resolveActiveContract(contracts);
        Room activeRoom = activeContract == null ? null : activeContract.getRoom();
        List<Long> roomIds = contracts.stream()
            .map(RentalContract::getRoom)
            .filter(Objects::nonNull)
            .map(Room::getId)
            .distinct()
            .toList();

        TenantResponse tenantResponse = tenantService.toResponse(tenant);
        RoomResponse roomResponse = activeRoom == null ? null : RoomMapper.toResponse(activeRoom);
        ContractResponse contractResponse = activeContract == null ? null : contractService.toResponse(activeContract);
        List<InvoiceResponse> invoices = invoiceRepository
            .findDistinctByContractTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(userAccountId)
            .stream()
            .map(invoiceService::toResponse)
            .toList();
        List<PaymentResponse> payments = paymentRepository
            .findDistinctByInvoiceContractTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByPaidAtDesc(userAccountId)
            .stream()
            .map(paymentService::toResponse)
            .toList();
        List<UtilityReadingResponse> utilityReadings = roomIds.isEmpty()
            ? List.of()
            : utilityReadingRepository.findByRoomIdInAndDeletedAtIsNullOrderByBillingYearDescBillingMonthDesc(roomIds)
                .stream()
                .filter(reading -> isVisibleToTenant(reading, contracts, tenant.getId()))
                .map(utilityReadingService::toResponse)
                .toList();
        List<MaintenanceRequestResponse> maintenanceRequests = maintenanceRepository
            .findByTenantUserAccountIdAndDeletedAtIsNullOrderBySubmittedAtDesc(userAccountId)
            .stream()
            .map(maintenanceService::toResponse)
            .toList();

        return new TenantPortalSummaryResponse(
            tenantResponse,
            roomResponse,
            contractResponse,
            invoices,
            payments,
            utilityReadings,
            maintenanceRequests
        );
    }

    @Transactional
    public MaintenanceRequestResponse createMaintenance(TenantMaintenanceCreateRequest request) {
        Tenant tenant = tenantService.getCurrentTenant();
        RentalContract activeContract = resolveActiveContract(contractRepository
            .findDistinctByTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByCreatedAtDesc(currentUserService.currentUserId()));
        if (activeContract == null || activeContract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Tenant does not have an active contract");
        }
        return maintenanceService.createForTenant(
            activeContract.getRoom(),
            tenant,
            request.title(),
            request.description(),
            request.priority()
        );
    }

    private RentalContract resolveActiveContract(List<RentalContract> contracts) {
        return contracts.stream()
            .filter(contract -> contract.getStatus() == ContractStatus.ACTIVE)
            .findFirst()
            .orElse(contracts.isEmpty() ? null : contracts.get(0));
    }

    private boolean isVisibleToTenant(
        UtilityReading reading,
        List<RentalContract> contracts,
        Long tenantId
    ) {
        YearMonth billingPeriod = YearMonth.of(reading.getBillingYear(), reading.getBillingMonth());
        return contracts.stream().anyMatch(contract -> {
            if (!contract.getRoom().getId().equals(reading.getRoom().getId())) {
                return false;
            }
            if (reading.getInvoice() != null) {
                if (!reading.getInvoice().getContract().getId().equals(contract.getId())) {
                    return false;
                }
            } else if (contract.getStatus() != ContractStatus.ACTIVE) {
                return false;
            }

            ContractTenant membership = contract.getTenants().stream()
                .filter(item -> item.getTenant().getId().equals(tenantId))
                .findFirst()
                .orElse(null);
            if (membership == null) {
                return false;
            }
            LocalDate moveIn = membership.getMoveInDate() == null
                ? contract.getStartDate()
                : membership.getMoveInDate();
            LocalDate moveOut = membership.getMoveOutDate() == null
                ? contract.getEndDate()
                : membership.getMoveOutDate();
            return !billingPeriod.isBefore(YearMonth.from(moveIn))
                && (moveOut == null || !billingPeriod.isAfter(YearMonth.from(moveOut)));
        });
    }
}
