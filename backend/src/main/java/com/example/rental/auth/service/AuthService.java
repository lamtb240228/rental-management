package com.example.rental.auth.service;

import com.example.rental.auth.dto.AuthResponse;
import com.example.rental.auth.dto.ChangePasswordRequest;
import com.example.rental.auth.dto.LoginRequest;
import com.example.rental.auth.dto.RegisterRequest;
import com.example.rental.auth.dto.UserProfileResponse;
import com.example.rental.auth.entity.Role;
import com.example.rental.auth.entity.RoleName;
import com.example.rental.auth.repository.RoleRepository;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.ForbiddenException;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.exception.UnauthorizedException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.common.security.JwtService;
import com.example.rental.common.security.UserPrincipal;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Email or password is incorrect";

    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;
    private final RefreshSessionService refreshSessionService;

    public AuthService(
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        CurrentUserService currentUserService,
        RefreshSessionService refreshSessionService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-not-used-for-authentication");
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.currentUserService = currentUserService;
        this.refreshSessionService = refreshSessionService;
    }

    @Transactional
    public AuthenticatedSession register(RegisterRequest request, String currentRefreshToken) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        Role landlordRole = roleRepository.findByName(RoleName.LANDLORD)
            .orElseThrow(() -> new NotFoundException("Default LANDLORD role is missing"));

        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.getRoles().add(landlordRole);
        try {
            userAccountRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email is already registered");
        }

        revokeCurrentBrowserSession(currentRefreshToken);
        return createAuthenticatedSession(user);
    }

    @Transactional
    public AuthenticatedSession login(LoginRequest request, String currentRefreshToken) {
        String normalizedEmail = normalizeEmail(request.email());
        UserAccount user = lockLoginContext(normalizedEmail, currentRefreshToken)
            .orElseGet(() -> {
                passwordMatches(request.password(), dummyPasswordHash);
                throw invalidCredentials();
            });
        boolean credentialsMatch = passwordMatches(request.password(), user.getPasswordHash());
        if (!credentialsMatch || !user.isActive() || !normalizeEmail(user.getEmail()).equals(normalizedEmail)) {
            throw invalidCredentials();
        }
        user.markLoggedIn();
        revokeCurrentBrowserSession(currentRefreshToken);
        return createAuthenticatedSession(user);
    }

    public AuthenticatedSession refresh(String rawRefreshToken) {
        RefreshSessionGrant refreshSession = refreshSessionService.rotate(rawRefreshToken);
        return createAuthenticatedSession(refreshSession.userAccount(), refreshSession);
    }

    public void logout(String rawRefreshToken) {
        refreshSessionService.revokeFamilyForToken(rawRefreshToken);
    }

    public void logoutAll(String rawRefreshToken) {
        refreshSessionService.revokeAllForToken(rawRefreshToken);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserAccount user = userAccountRepository.findByIdAndDeletedAtIsNullForUpdate(currentUserService.currentUserId())
            .filter(UserAccount::isActive)
            .orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        if (!passwordMatches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordMatches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshSessionService.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me() {
        UserAccount user = userAccountRepository.findById(currentUserService.currentUserId())
            .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));
        return toProfile(user);
    }

    private UserProfileResponse toProfile(UserAccount user) {
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), roles);
    }

    private AuthenticatedSession createAuthenticatedSession(UserAccount user) {
        return createAuthenticatedSession(user, refreshSessionService.issue(user));
    }

    private AuthenticatedSession createAuthenticatedSession(UserAccount user, RefreshSessionGrant refreshSession) {
        UserPrincipal principal = UserPrincipal.from(user);
        AuthResponse response = new AuthResponse(jwtService.generateToken(principal), "Bearer", toProfile(user));
        return new AuthenticatedSession(response, refreshSession);
    }

    private void revokeCurrentBrowserSession(String currentRefreshToken) {
        if (currentRefreshToken != null && !currentRefreshToken.isBlank()) {
            refreshSessionService.revokeFamilyForToken(currentRefreshToken);
        }
    }

    private Optional<UserAccount> lockLoginContext(String normalizedEmail, String currentRefreshToken) {
        Optional<Long> loginUserId = userAccountRepository.findIdByEmailIgnoreCase(normalizedEmail);
        if (loginUserId.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> userIdsToLock = new TreeSet<>();
        userIdsToLock.add(loginUserId.get());
        refreshSessionService.resolveUserAccountId(currentRefreshToken).ifPresent(userIdsToLock::add);

        UserAccount loginUser = null;
        for (Long userAccountId : userIdsToLock) {
            Optional<UserAccount> lockedUser = userAccountRepository.findByIdForUpdate(userAccountId);
            if (userAccountId.equals(loginUserId.get()) && lockedUser.isPresent()) {
                loginUser = lockedUser.get();
            }
        }
        return Optional.ofNullable(loginUser);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(INVALID_CREDENTIALS);
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
