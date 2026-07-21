package com.example.rental.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.filter.OncePerRequestFilter;

public class CookieAuthOriginFilter extends OncePerRequestFilter {
    private static final Set<String> PROTECTED_PATHS = Set.of(
        "/api/auth/refresh",
        "/api/auth/logout",
        "/api/auth/logout-all"
    );

    private final TrustedOriginPolicy trustedOriginPolicy;
    private final RestSecurityExceptionHandler securityExceptionHandler;

    public CookieAuthOriginFilter(
        TrustedOriginPolicy trustedOriginPolicy,
        RestSecurityExceptionHandler securityExceptionHandler
    ) {
        this.trustedOriginPolicy = trustedOriginPolicy;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        // Use Spring's decoded application path, matching the representation
        // used for MVC handler matching. Comparing raw requestURI would let an
        // encoded unreserved character (for example %72efresh) skip this filter
        // and then decode to a protected controller mapping.
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        // Prefix matching also protects path-parameter/suffix variants before
        // MVC or the firewall decides whether a handler is available.
        return PROTECTED_PATHS.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        List<String> origins = Collections.list(request.getHeaders(HttpHeaders.ORIGIN));
        boolean allowed;
        if (!origins.isEmpty()) {
            allowed = origins.size() == 1 && trustedOriginPolicy.allowsOrigin(origins.get(0));
        } else {
            List<String> referers = Collections.list(request.getHeaders(HttpHeaders.REFERER));
            allowed = referers.size() == 1 && trustedOriginPolicy.allowsReferer(referers.get(0));
        }

        if (!allowed) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            securityExceptionHandler.handle(request, response, new AccessDeniedException("Untrusted request origin"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
