package com.example.rental.common.security;

import com.example.rental.common.config.CorsProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class TrustedOriginPolicy {
    private final List<String> allowedOrigins;
    private final Set<String> allowedOriginSet;

    public TrustedOriginPolicy(CorsProperties properties) {
        LinkedHashSet<String> normalizedOrigins = new LinkedHashSet<>();
        Arrays.stream(properties.allowedOrigins().split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .map(TrustedOriginPolicy::requireOrigin)
            .forEach(normalizedOrigins::add);

        if (normalizedOrigins.isEmpty()) {
            throw new IllegalArgumentException("CORS allowed origins must contain explicit origins");
        }
        allowedOrigins = List.copyOf(normalizedOrigins);
        allowedOriginSet = Set.copyOf(normalizedOrigins);
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    public boolean allowsOrigin(String origin) {
        String normalized = normalize(origin, false);
        return normalized != null && allowedOriginSet.contains(normalized);
    }

    public boolean allowsReferer(String referer) {
        String normalized = normalize(referer, true);
        return normalized != null && allowedOriginSet.contains(normalized);
    }

    private static String requireOrigin(String origin) {
        String normalized = normalize(origin, false);
        if (normalized == null) {
            throw new IllegalArgumentException("CORS allowed origins must be explicit HTTP(S) origins");
        }
        return normalized;
    }

    private static String normalize(String value, boolean allowResourcePath) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                return null;
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            if (!allowResourcePath
                && ((uri.getRawPath() != null && !uri.getRawPath().isEmpty()) || uri.getRawQuery() != null)) {
                return null;
            }

            int port = uri.getPort();
            if (port == 0 || port > 65_535) {
                return null;
            }
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            host = host.toLowerCase(Locale.ROOT);
            if (host.contains(":") && !host.startsWith("[")) {
                host = "[" + host + "]";
            }
            return scheme + "://" + host + (port < 0 ? "" : ":" + port);
        } catch (URISyntaxException exception) {
            return null;
        }
    }
}
