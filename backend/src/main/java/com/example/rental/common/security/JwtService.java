package com.example.rental.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.expirationMinutes() * 60);
        return Jwts.builder()
            .subject(principal.getUsername())
            .id(UUID.randomUUID().toString())
            .claim("userId", principal.getId())
            .claim("authVersion", principal.getAuthVersion())
            .claim("roles", principal.getAuthorities().stream().map(Object::toString).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        Object authVersionClaim = claims.get("authVersion");
        return claims.getSubject().equals(principal.getUsername())
            && authVersionClaim instanceof Number authVersion
            && authVersion.longValue() == principal.getAuthVersion()
            && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
