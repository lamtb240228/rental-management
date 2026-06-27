package com.example.rental.auth.service;

import com.example.rental.auth.dto.AuthResponse;
import com.example.rental.auth.dto.LoginRequest;
import com.example.rental.auth.dto.RegisterRequest;
import com.example.rental.auth.dto.UserProfileResponse;
import com.example.rental.auth.entity.Role;
import com.example.rental.auth.entity.RoleName;
import com.example.rental.auth.repository.RoleRepository;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.ForbiddenException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.common.security.JwtService;
import com.example.rental.common.security.UserPrincipal;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.Set;
import java.util.stream.Collectors;
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

    public AuthService(
        AuthenticationManager authenticationManager,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        CurrentUserService currentUserService
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        Role landlordRole = roleRepository.findByName(RoleName.LANDLORD)
            .orElseThrow(() -> new NotFoundException("Default LANDLORD role is missing"));

        UserAccount user = new UserAccount();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.getRoles().add(landlordRole);
        userAccountRepository.save(user);

        UserPrincipal principal = UserPrincipal.from(user);
        return new AuthResponse(jwtService.generateToken(principal), "Bearer", toProfile(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserAccount user = userAccountRepository.findById(principal.getId())
            .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));
        user.markLoggedIn();
        return new AuthResponse(jwtService.generateToken(principal), "Bearer", toProfile(user));
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
}
