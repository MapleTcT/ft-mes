package com.mapletct.ftmes.processanalysis.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class TenantResolver {

    private static final String[] TENANT_HEADERS = {
        "X-Tenant-Id", "tenantId", "Tenant-Id", "Supos-Tenant-Id"
    };

    private static final String[] JWT_CLAIMS = {
        "tenantId", "tenant_id", "tenant", "companyCode", "company_code"
    };

    private final String defaultTenant;
    private final ObjectMapper objectMapper;

    public TenantResolver(
            @Value("${process-analysis.default-tenant:default}") String defaultTenant,
            ObjectMapper objectMapper) {
        this.defaultTenant = defaultTenant;
        this.objectMapper = objectMapper;
    }

    public String resolve(HttpServletRequest request) {
        for (String headerName : TENANT_HEADERS) {
            String value = request.getHeader(headerName);
            if (value != null && !value.trim().isEmpty()) {
                return limit(value.trim(), 64);
            }
        }
        String jwtTenant = tenantFromJwt(request.getHeader("Authorization"));
        if (!jwtTenant.isEmpty()) {
            return limit(jwtTenant, 64);
        }
        String normalizedDefault = defaultTenant == null ? "" : defaultTenant.trim();
        return normalizedDefault.isEmpty() ? "default" : limit(normalizedDefault, 64);
    }

    private String tenantFromJwt(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        String[] parts = authorization.substring(7).split("\\.");
        if (parts.length < 2) {
            return "";
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(
                new String(decoded, StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() { });
            for (String claim : JWT_CLAIMS) {
                Object value = claims.get(claim);
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    return String.valueOf(value).trim();
                }
            }
        } catch (Exception ignored) {
            // Authentication is enforced upstream; malformed metadata falls back to configured tenant.
        }
        return "";
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
