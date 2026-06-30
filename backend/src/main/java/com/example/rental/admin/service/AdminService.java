package com.example.rental.admin.service;

import com.example.rental.admin.dto.AdminSummaryResponse;
import com.example.rental.admin.dto.AdminUserResponse;
import com.example.rental.admin.dto.AdminUserStatusUpdateRequest;
import com.example.rental.auth.entity.RoleName;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.maintenance.entity.MaintenanceStatus;
import com.example.rental.maintenance.repository.MaintenanceRequestRepository;
import com.example.rental.property.repository.PropertyRepository;
import com.example.rental.property.repository.RoomRepository;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserAccountRepository userAccountRepository;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final CurrentUserService currentUserService;

    public AdminService(
        UserAccountRepository userAccountRepository,
        PropertyRepository propertyRepository,
        RoomRepository roomRepository,
        InvoiceRepository invoiceRepository,
        MaintenanceRequestRepository maintenanceRepository,
        CurrentUserService currentUserService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(
            userAccountRepository.countByDeletedAtIsNull(),
            userAccountRepository.countDistinctByRolesNameAndDeletedAtIsNull(RoleName.LANDLORD),
            userAccountRepository.countDistinctByRolesNameAndDeletedAtIsNull(RoleName.TENANT),
            propertyRepository.countByDeletedAtIsNull(),
            roomRepository.countByDeletedAtIsNull(),
            invoiceRepository.countByDeletedAtIsNull(),
            maintenanceRepository.countByStatusAndDeletedAtIsNull(MaintenanceStatus.PENDING)
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return userAccountRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long id, AdminUserStatusUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(id)
            .filter(account -> account.getDeletedAt() == null)
            .orElseThrow(() -> new NotFoundException("User account not found"));
        if (id.equals(currentUserService.currentUserId()) && request.status() != user.getStatus()) {
            throw new BadRequestException("You cannot change your own account status");
        }
        user.setStatus(request.status());
        return toResponse(user);
    }

    private AdminUserResponse toResponse(UserAccount user) {
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getStatus(),
            roles,
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
