package com.example.rental.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.rental.admin.dto.AdminUserStatusUpdateRequest;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.common.config.DemoDataProperties;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.maintenance.repository.MaintenanceRequestRepository;
import com.example.rental.property.repository.PropertyRepository;
import com.example.rental.property.repository.RoomRepository;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private MaintenanceRequestRepository maintenanceRepository;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void demoAccountStatusCannotBeChangedWhenDemoDataIsDisabled() {
        UserAccount demoAccount = new UserAccount();
        demoAccount.setEmail("ADMIN@RENTAL.LOCAL");
        demoAccount.setStatus(UserStatus.LOCKED);
        when(userAccountRepository.findById(100L)).thenReturn(Optional.of(demoAccount));

        AdminService service = new AdminService(
            userAccountRepository,
            propertyRepository,
            roomRepository,
            invoiceRepository,
            maintenanceRepository,
            currentUserService,
            new DemoDataProperties(false)
        );

        assertThatThrownBy(() -> service.updateUserStatus(
            100L,
            new AdminUserStatusUpdateRequest(UserStatus.ACTIVE)
        ))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Demo account status is immutable when demo data is disabled");
    }
}
