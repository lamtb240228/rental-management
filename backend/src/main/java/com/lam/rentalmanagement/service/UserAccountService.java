package com.lam.rentalmanagement.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lam.rentalmanagement.domain.entity.Role;
import com.lam.rentalmanagement.domain.entity.UserAccount;
import com.lam.rentalmanagement.domain.entity.UserAccountStatus;
import com.lam.rentalmanagement.dto.ChangePasswordRequest;
import com.lam.rentalmanagement.dto.CreateUserAccountRequest;
import com.lam.rentalmanagement.dto.UpdateUserAccountRequest;
import com.lam.rentalmanagement.dto.UserAccountResponse;
import com.lam.rentalmanagement.exception.DuplicateEmailException;
import com.lam.rentalmanagement.exception.PasswordConfirmationMismatchException;
import com.lam.rentalmanagement.exception.RoleNotFoundException;
import com.lam.rentalmanagement.exception.UserAccountNotFoundException;
import com.lam.rentalmanagement.repository.RoleRepository;
import com.lam.rentalmanagement.repository.UserAccountRepository;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccountResponse> getAllUserAccounts() {
        return userAccountRepository.findAll()
                .stream()
                .map(UserAccountResponse::from)
                .toList();
    }

    public UserAccountResponse getUserAccountById(Long id) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(
                        () -> new UserAccountNotFoundException(id)
                );

        return UserAccountResponse.from(userAccount);
    }

    @Transactional
    public UserAccountResponse createUserAccount(
            CreateUserAccountRequest request
    ) {
        String normalizedEmail = normalizeEmail(request.email());

        String normalizedRoleName = request.roleName()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(
                        () -> new RoleNotFoundException(normalizedRoleName)
                );

        String passwordHash =
                passwordEncoder.encode(request.password());

        String normalizedPhone =
                normalizePhone(request.phone());

        UserAccount userAccount = new UserAccount(
                normalizedEmail,
                passwordHash,
                request.fullName().trim(),
                normalizedPhone
        );

        userAccount.addRole(role);

        UserAccount savedUserAccount =
                userAccountRepository.save(userAccount);

        return UserAccountResponse.from(savedUserAccount);
    }

    @Transactional
    public UserAccountResponse updateUserAccount(
            Long id,
            UpdateUserAccountRequest request
    ) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(
                        () -> new UserAccountNotFoundException(id)
                );

        String normalizedEmail =
                normalizeEmail(request.email());

        boolean emailUsedByAnotherAccount =
                userAccountRepository.existsByEmailAndIdNot(
                        normalizedEmail,
                        id
                );

        if (emailUsedByAnotherAccount) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        userAccount.setEmail(normalizedEmail);
        userAccount.setFullName(request.fullName().trim());
        userAccount.setPhone(normalizePhone(request.phone()));
        userAccount.setStatus(request.status());

        UserAccount savedUserAccount =
                userAccountRepository.save(userAccount);

        return UserAccountResponse.from(savedUserAccount);
    }

    @Transactional
    public void changePassword(
            Long id,
            ChangePasswordRequest request
    ) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(
                        () -> new UserAccountNotFoundException(id)
                );

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new PasswordConfirmationMismatchException();
        }

        String newPasswordHash =
                passwordEncoder.encode(request.newPassword());

        userAccount.setPasswordHash(newPasswordHash);

        userAccountRepository.save(userAccount);
    }

    @Transactional
    public void lockUserAccount(Long id) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(
                        () -> new UserAccountNotFoundException(id)
                );

        userAccount.setStatus(UserAccountStatus.LOCKED);

        userAccountRepository.save(userAccount);
    }

    @Transactional
    public void unlockUserAccount(Long id) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(
                        () -> new UserAccountNotFoundException(id)
                );

        userAccount.setStatus(UserAccountStatus.ACTIVE);

        userAccountRepository.save(userAccount);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }
}