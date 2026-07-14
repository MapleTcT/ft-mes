package com.mapletct.ftmes.bpiadapter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "bpi.adapter")
public class BpiAdapterProperties implements InitializingBean {

    private String upstreamBaseUrl = "http://bpi-service:19091";
    private String keycloakJwkSetUri;
    private String keycloakIssuer;
    private String keycloakAudience = "pc_dt";
    private boolean legacyTicketEnabled = true;
    private String legacyGatewayBaseUrl;
    private String tenantClaim = "companyId";
    private String usernameClaim = "userName";
    private String internalJwtSecret;
    private String internalJwtIssuer = "ft-mes-adapter";
    private String internalJwtAudience = "bpi-service";
    private Duration internalTokenTtl = Duration.ofMinutes(10);
    private String roleRules;
    private String subjectScopeRules;
    private Map<String, List<String>> roleMappings = new LinkedHashMap<String, List<String>>();
    private Map<String, SubjectScope> subjectScopes = new LinkedHashMap<String, SubjectScope>();

    @Override
    public void afterPropertiesSet() {
        requireHttpUri("upstream-base-url", upstreamBaseUrl);
        requireHttpUri("keycloak-jwk-set-uri", keycloakJwkSetUri);
        requireText("keycloak-issuer", keycloakIssuer);
        requireText("keycloak-audience", keycloakAudience);
        if (legacyTicketEnabled) requireHttpUri("legacy-gateway-base-url", legacyGatewayBaseUrl);
        requireText("internal-jwt-issuer", internalJwtIssuer);
        requireText("internal-jwt-audience", internalJwtAudience);
        if (internalJwtSecret == null || internalJwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("bpi.adapter.internal-jwt-secret must contain at least 32 UTF-8 bytes");
        }
        if (internalTokenTtl == null || internalTokenTtl.isZero() || internalTokenTtl.isNegative()
                || internalTokenTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalStateException("bpi.adapter.internal-token-ttl must be between 1 second and 15 minutes");
        }
        if ((roleMappings == null || roleMappings.isEmpty()) && hasText(roleRules)) parseRoleRules();
        if ((subjectScopes == null || subjectScopes.isEmpty()) && hasText(subjectScopeRules)) parseSubjectScopeRules();
        if (roleMappings == null || roleMappings.isEmpty()) {
            throw new IllegalStateException("bpi.adapter.role-mappings must contain at least one approved legacy role");
        }
        if (subjectScopes == null || subjectScopes.isEmpty()) {
            throw new IllegalStateException("bpi.adapter.subject-scopes must contain at least one explicit subject scope");
        }
        for (Map.Entry<String, SubjectScope> entry : subjectScopes.entrySet()) {
            SubjectScope scope = entry.getValue();
            if (scope == null || scope.getTenantIds().isEmpty() || scope.getPlantIds().isEmpty() || scope.getLineIds().isEmpty()) {
                throw new IllegalStateException("bpi.adapter.subject-scopes." + entry.getKey()
                        + " must define tenant-ids, plant-ids, and line-ids");
            }
        }
    }

    private void parseRoleRules() {
        for (String rule : roleRules.split(";")) {
            String[] pair = rule.split("=", 2);
            if (pair.length != 2 || !hasText(pair[0])) {
                throw new IllegalStateException("bpi.adapter.role-rules contains a malformed rule");
            }
            List<String> roles = splitValues(pair[1], "\\|");
            if (roles.isEmpty()) throw new IllegalStateException("bpi.adapter.role-rules contains an empty role list");
            roleMappings.put(pair[0].trim(), roles);
        }
    }

    private void parseSubjectScopeRules() {
        for (String rule : subjectScopeRules.split(";")) {
            String[] pair = rule.split("=", 2);
            String[] values = pair.length == 2 ? pair[1].split("\\|", -1) : new String[0];
            if (pair.length != 2 || !hasText(pair[0]) || values.length != 3) {
                throw new IllegalStateException("bpi.adapter.subject-scope-rules contains a malformed rule");
            }
            SubjectScope scope = new SubjectScope();
            scope.setTenantIds(splitValues(values[0], ","));
            scope.setPlantIds(splitValues(values[1], ","));
            scope.setLineIds(splitValues(values[2], ","));
            subjectScopes.put(pair[0].trim(), scope);
        }
    }

    private List<String> splitValues(String value, String separator) {
        List<String> result = new ArrayList<String>();
        for (String item : Arrays.asList(value.split(separator))) {
            if (hasText(item)) result.add(item.trim());
        }
        return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void requireHttpUri(String name, String value) {
        requireText(name, value);
        URI uri = URI.create(value);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalStateException("bpi.adapter." + name + " must be an absolute HTTP(S) URI");
        }
    }

    private void requireText(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("bpi.adapter." + name + " is required");
        }
    }

    public String getUpstreamBaseUrl() { return upstreamBaseUrl; }
    public void setUpstreamBaseUrl(String upstreamBaseUrl) { this.upstreamBaseUrl = upstreamBaseUrl; }
    public String getKeycloakJwkSetUri() { return keycloakJwkSetUri; }
    public void setKeycloakJwkSetUri(String keycloakJwkSetUri) { this.keycloakJwkSetUri = keycloakJwkSetUri; }
    public String getKeycloakIssuer() { return keycloakIssuer; }
    public void setKeycloakIssuer(String keycloakIssuer) { this.keycloakIssuer = keycloakIssuer; }
    public String getKeycloakAudience() { return keycloakAudience; }
    public void setKeycloakAudience(String keycloakAudience) { this.keycloakAudience = keycloakAudience; }
    public boolean isLegacyTicketEnabled() { return legacyTicketEnabled; }
    public void setLegacyTicketEnabled(boolean legacyTicketEnabled) { this.legacyTicketEnabled = legacyTicketEnabled; }
    public String getLegacyGatewayBaseUrl() { return legacyGatewayBaseUrl; }
    public void setLegacyGatewayBaseUrl(String legacyGatewayBaseUrl) { this.legacyGatewayBaseUrl = legacyGatewayBaseUrl; }
    public String getTenantClaim() { return tenantClaim; }
    public void setTenantClaim(String tenantClaim) { this.tenantClaim = tenantClaim; }
    public String getUsernameClaim() { return usernameClaim; }
    public void setUsernameClaim(String usernameClaim) { this.usernameClaim = usernameClaim; }
    public String getInternalJwtSecret() { return internalJwtSecret; }
    public void setInternalJwtSecret(String internalJwtSecret) { this.internalJwtSecret = internalJwtSecret; }
    public String getInternalJwtIssuer() { return internalJwtIssuer; }
    public void setInternalJwtIssuer(String internalJwtIssuer) { this.internalJwtIssuer = internalJwtIssuer; }
    public String getInternalJwtAudience() { return internalJwtAudience; }
    public void setInternalJwtAudience(String internalJwtAudience) { this.internalJwtAudience = internalJwtAudience; }
    public Duration getInternalTokenTtl() { return internalTokenTtl; }
    public void setInternalTokenTtl(Duration internalTokenTtl) { this.internalTokenTtl = internalTokenTtl; }
    public String getRoleRules() { return roleRules; }
    public void setRoleRules(String roleRules) { this.roleRules = roleRules; }
    public String getSubjectScopeRules() { return subjectScopeRules; }
    public void setSubjectScopeRules(String subjectScopeRules) { this.subjectScopeRules = subjectScopeRules; }
    public Map<String, List<String>> getRoleMappings() { return roleMappings; }
    public void setRoleMappings(Map<String, List<String>> roleMappings) { this.roleMappings = roleMappings; }
    public Map<String, SubjectScope> getSubjectScopes() { return subjectScopes; }
    public void setSubjectScopes(Map<String, SubjectScope> subjectScopes) { this.subjectScopes = subjectScopes; }

    public static class SubjectScope {
        private List<String> tenantIds = new ArrayList<String>();
        private List<String> plantIds = new ArrayList<String>();
        private List<String> lineIds = new ArrayList<String>();

        public List<String> getTenantIds() { return tenantIds; }
        public void setTenantIds(List<String> tenantIds) { this.tenantIds = tenantIds; }
        public List<String> getPlantIds() { return plantIds; }
        public void setPlantIds(List<String> plantIds) { this.plantIds = plantIds; }
        public List<String> getLineIds() { return lineIds; }
        public void setLineIds(List<String> lineIds) { this.lineIds = lineIds; }
    }
}
