package com.example.rental.tenant.service;

import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.tenant.dto.TenantRequest;
import com.example.rental.tenant.dto.TenantResponse;
import com.example.rental.tenant.entity.Tenant;
import com.example.rental.tenant.entity.TenantStatus;
import com.example.rental.tenant.repository.TenantRepository;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final RentalContractRepository contractRepository;

    public TenantService(
        TenantRepository tenantRepository,
        UserAccountRepository userAccountRepository,
        CurrentUserService currentUserService,
        RentalContractRepository contractRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listMine() {
        return tenantRepository.findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(currentUserService.currentUserId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return toResponse(getOwnedTenant(id));
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        Long landlordId = currentUserService.currentUserId();
        ensureIdentityUnique(landlordId, request.identityNumber(), null);
        UserAccount landlord = userAccountRepository.findById(landlordId)
            .orElseThrow(() -> new NotFoundException("Current user not found"));

        Tenant tenant = new Tenant();
        tenant.setLandlord(landlord);
        apply(tenant, request);
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse update(Long id, TenantRequest request) {
        Tenant tenant = getOwnedTenantForUpdate(id);
        if (request.status() == TenantStatus.INACTIVE
            && contractRepository.existsDistinctByTenantsTenantIdAndStatusAndDeletedAtIsNull(id, ContractStatus.ACTIVE)) {
            throw new BadRequestException("Cannot deactivate a tenant with an active contract");
        }
        ensureIdentityUnique(currentUserService.currentUserId(), request.identityNumber(), tenant.getIdentityNumber());
        apply(tenant, request);
        return toResponse(tenant);
    }

    @Transactional
    public void delete(Long id) {
        Tenant tenant = getOwnedTenantForUpdate(id);
        if (contractRepository.existsDistinctByTenantsTenantIdAndStatusAndDeletedAtIsNull(id, ContractStatus.ACTIVE)) {
            throw new BadRequestException("Cannot remove a tenant with an active contract");
        }
        tenant.softDelete();
    }

    public Tenant getOwnedTenant(Long id) {
        return tenantRepository.findByIdAndLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Tenant not found"));
    }

    public Tenant getCurrentTenant() {
        return tenantRepository.findByUserAccountIdAndDeletedAtIsNull(currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Tenant profile not found"));
    }

    private Tenant getOwnedTenantForUpdate(Long id) {
        return tenantRepository.findOwnedByIdForUpdate(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Tenant not found"));
    }

    private void ensureIdentityUnique(Long landlordId, String identityNumber, String currentIdentityNumber) {
        if (!StringUtils.hasText(identityNumber) || identityNumber.equals(currentIdentityNumber)) {
            return;
        }
        if (tenantRepository.existsByLandlordIdAndIdentityNumberAndDeletedAtIsNull(landlordId, identityNumber)) {
            throw new ConflictException("Identity number already exists");
        }
    }

    private void apply(Tenant tenant, TenantRequest request) {
        tenant.setFullName(request.fullName().trim());
        tenant.setDateOfBirth(request.dateOfBirth());
        tenant.setPhone(request.phone());
        tenant.setEmail(request.email());
        tenant.setIdentityNumber(request.identityNumber());
        tenant.setPermanentAddress(request.permanentAddress());
        tenant.setStatus(request.status() == null ? TenantStatus.ACTIVE : request.status());
    }

    public TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getFullName(),
            tenant.getDateOfBirth(),
            tenant.getPhone(),
            tenant.getEmail(),
            tenant.getIdentityNumber(),
            tenant.getPermanentAddress(),
            tenant.getStatus(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }
}
