package com.mapletct.ftmes.womquality.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public final class RequestContext {

    private static final String[] TENANT_HEADERS = {
        "X-Tenant-Id", "tenantId", "Tenant-Id", "Supos-Tenant-Id"
    };

    private final ObjectMapper objectMapper;

    public RequestContext(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String tenant(HttpServletRequest request) {
        for (String header : TENANT_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.trim().isEmpty()) {
                return limit(value.trim(), 64);
            }
        }
        String claim = jwtClaim(request, "tenantId", "tenant_id", "tenant", "companyCode");
        return claim.isEmpty() ? "default" : limit(claim, 64);
    }

    public String actor(HttpServletRequest request) {
        String header = firstHeader(request, "X-User-Name", "X-Staff-Code", "X-ADP-User");
        if (!header.isEmpty()) {
            return limit(header, 128);
        }
        String claim = jwtClaim(request, "preferred_username", "username", "user_name", "sub");
        return claim.isEmpty() ? "authenticated-user" : limit(claim, 128);
    }

    private String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String jwtClaim(HttpServletRequest request, String... names) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        String[] parts = authorization.substring(7).split("\\.");
        if (parts.length < 2) {
            return "";
        }
        try {
            Map<String, Object> claims = objectMapper.readValue(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() { });
            for (String name : names) {
                Object value = claims.get(name);
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    return String.valueOf(value).trim();
                }
            }
        } catch (Exception ignored) {
            // Upstream auth remains authoritative; malformed optional claims use safe fallbacks.
        }
        return "";
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
