package com.example.rental.auth.service;

import com.example.rental.auth.dto.AuthResponse;

public final class AuthenticatedSession {
    private final AuthResponse response;
    private final RefreshSessionGrant refreshSession;

    AuthenticatedSession(AuthResponse response, RefreshSessionGrant refreshSession) {
        this.response = response;
        this.refreshSession = refreshSession;
    }

    public AuthResponse response() {
        return response;
    }

    public RefreshSessionGrant refreshSession() {
        return refreshSession;
    }

    @Override
    public String toString() {
        return "AuthenticatedSession[response=" + response + ", refreshSession=" + refreshSession + "]";
    }
}
