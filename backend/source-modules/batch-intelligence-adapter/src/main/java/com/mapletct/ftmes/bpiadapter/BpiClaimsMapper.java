package com.mapletct.ftmes.bpiadapter;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BpiClaimsMapper {

    private final BpiAdapterProperties properties;

    public BpiClaimsMapper(BpiAdapterProperties properties) {
        this.properties = properties;
    }

    public BpiActor map(Jwt jwt) {
        String subject = required(jwt.getSubject(), "JWT subject is missing");
        String username = claimAsString(jwt, properties.getUsernameClaim());
        if (username == null) username = claimAsString(jwt, "preferred_username");
        if (username == null) username = subject;

        String tenantId = claimAsString(jwt, properties.getTenantClaim());
        tenantId = required(tenantId, "JWT tenant claim is missing");

        BpiAdapterProperties.SubjectScope scope = properties.getSubjectScopes().get(username);
        if (scope == null) scope = properties.getSubjectScopes().get(subject);
        if (scope == null) throw new AdapterAccessDeniedException("No server-side BPI scope is configured for this subject");
        if (!scope.getTenantIds().contains("*") && !scope.getTenantIds().contains(tenantId)) {
            throw new AdapterAccessDeniedException("The subject scope does not allow this tenant");
        }

        Set<String> mappedRoles = new LinkedHashSet<String>();
        for (String legacyRole : legacyRoles(jwt)) {
            List<String> roles = properties.getRoleMappings().get(legacyRole);
            if (roles != null) mappedRoles.addAll(roles);
        }
        if (mappedRoles.isEmpty()) {
            throw new AdapterAccessDeniedException("No approved BPI role mapping matched the authenticated subject");
        }
        if (scope.getPlantIds().isEmpty() || scope.getLineIds().isEmpty()) {
            throw new AdapterAccessDeniedException("The server-side BPI plant/line scope is empty");
        }
        return new BpiActor(subject, tenantId, mappedRoles,
                new LinkedHashSet<String>(scope.getPlantIds()), new LinkedHashSet<String>(scope.getLineIds()));
    }

    private Set<String> legacyRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<String>();
        addStrings(roles, jwt.getClaims().get("roles"));
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map) addStrings(roles, ((Map<?, ?>) realmAccess).get("roles"));
        return roles;
    }

    private void addStrings(Set<String> target, Object value) {
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) if (item != null) target.add(String.valueOf(item));
        } else if (value instanceof String) {
            target.add((String) value);
        }
    }

    private String claimAsString(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        return value == null ? null : String.valueOf(value);
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new AdapterAccessDeniedException(message);
        return value;
    }
}
