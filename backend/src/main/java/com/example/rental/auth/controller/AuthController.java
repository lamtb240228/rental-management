package com.example.rental.auth.controller;

import com.example.rental.auth.dto.AuthResponse;
import com.example.rental.auth.dto.ChangePasswordRequest;
import com.example.rental.auth.dto.LoginRequest;
import com.example.rental.auth.dto.RegisterRequest;
import com.example.rental.auth.dto.UserProfileResponse;
import com.example.rental.auth.service.AuthService;
import com.example.rental.auth.service.AuthenticatedSession;
import com.example.rental.common.response.ApiResponse;
import com.example.rental.common.exception.UnauthorizedException;
import com.example.rental.common.security.RefreshTokenCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        return sessionResponse(authService.register(request, readRefreshToken(httpRequest)), httpResponse);
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        return sessionResponse(authService.login(request, readRefreshToken(httpRequest)), httpResponse);
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        try {
            return sessionResponse(authService.refresh(readRefreshToken(request)), response);
        } catch (UnauthorizedException exception) {
            clearInvalidSession(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readRefreshToken(request));
        return clearSessionResponse(response);
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logoutAll(readRefreshToken(request));
            return clearSessionResponse(response);
        } catch (UnauthorizedException exception) {
            clearInvalidSession(response);
            throw exception;
        }
    }

    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        HttpServletResponse response
    ) {
        authService.changePassword(request);
        return clearSessionResponse(response);
    }

    @GetMapping("/me")
    ApiResponse<UserProfileResponse> me() {
        return ApiResponse.of(authService.me());
    }

    private ResponseEntity<ApiResponse<AuthResponse>> sessionResponse(
        AuthenticatedSession session,
        HttpServletResponse response
    ) {
        refreshTokenCookieService.write(response, session.refreshSession());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.of(session.response()));
    }

    private ResponseEntity<Void> clearSessionResponse(HttpServletResponse response) {
        refreshTokenCookieService.clear(response);
        return ResponseEntity.noContent()
            .cacheControl(CacheControl.noStore())
            .build();
    }

    private void clearInvalidSession(HttpServletResponse response) {
        refreshTokenCookieService.clear(response);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (refreshTokenCookieService.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
