package com.lam.rentalmanagement.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lam.rentalmanagement.domain.entity.Role;
import com.lam.rentalmanagement.domain.entity.UserAccount;
import com.lam.rentalmanagement.dto.CreateUserAccountRequest;
import com.lam.rentalmanagement.dto.UserAccountResponse;
import com.lam.rentalmanagement.exception.DuplicateEmailException;
import com.lam.rentalmanagement.exception.RoleNotFoundException;
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

    @Transactional
    public UserAccountResponse createUserAccount(
            CreateUserAccountRequest request
    ) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

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

        String passwordHash = passwordEncoder.encode(request.password());

        String normalizedPhone = normalizePhone(request.phone());

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

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }
}