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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;
    private final RefreshSessionService refreshSessionService;

    public AuthService(
        AuthenticationManager authenticationManager,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        CurrentUserService currentUserService,
        RefreshSessionService refreshSessionService
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
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
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserAccount user = userAccountRepository.findByIdAndDeletedAtIsNullForUpdate(principal.getId())
            .filter(UserAccount::isActive)
            .orElseThrow(() -> new UnauthorizedException("Email or password is incorrect"));
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
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
