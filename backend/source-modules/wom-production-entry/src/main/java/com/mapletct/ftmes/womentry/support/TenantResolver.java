package com.mapletct.ftmes.womentry.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class TenantResolver {

    private final String defaultTenant;

    public TenantResolver(@Value("${wom-production-entry.default-tenant:default}") String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public String resolve(HttpServletRequest request) {
        String tenant = firstNonBlank(
            request.getHeader("X-Tenant-Id"),
            request.getHeader("tenantId"),
            request.getHeader("supfusion.tenantid")
        );
        return tenant == null ? defaultTenant : tenant;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
