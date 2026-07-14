package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BpiClaimsMapperTest {

    @Test
    public void mapsOnlyServerOwnedScopeAndApprovedRoles() {
        BpiAdapterProperties properties = properties();
        BpiActor actor = new BpiClaimsMapper(properties).map(jwt("admin"));

        assertEquals("1000", actor.getTenantId());
        assertEquals(Collections.singleton("PLANT-01"), actor.getPlantIds());
        assertEquals(Collections.singleton("LINE-S07-01"), actor.getLineIds());
        assertEquals(new java.util.LinkedHashSet<String>(Arrays.asList("BPI_ADMIN", "BPI_OPERATOR")), actor.getRoles());
    }

    @Test(expected = AdapterAccessDeniedException.class)
    public void rejectsAuthenticatedSubjectWithoutExplicitScope() {
        BpiAdapterProperties properties = properties();
        properties.setSubjectScopes(Collections.<String, BpiAdapterProperties.SubjectScope>emptyMap());
        new BpiClaimsMapper(properties).map(jwt("admin"));
    }

    @Test
    public void parsesEnvironmentFriendlyRoleAndScopeRules() {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        properties.setUpstreamBaseUrl("http://bpi-service:19091");
        properties.setKeycloakJwkSetUri("http://keycloak:8080/jwks");
        properties.setKeycloakIssuer("https://issuer.example/realms/dt");
        properties.setLegacyGatewayBaseUrl("http://gateway:8008");
        properties.setInternalJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setRoleRules("admin=BPI_ADMIN|BPI_OPERATOR");
        properties.setSubjectScopeRules("admin=1000|PLANT-01|LINE-S07-01");

        properties.afterPropertiesSet();

        assertEquals(Arrays.asList("BPI_ADMIN", "BPI_OPERATOR"), properties.getRoleMappings().get("admin"));
        assertEquals(Collections.singletonList("LINE-S07-01"), properties.getSubjectScopes().get("admin").getLineIds());
    }

    private BpiAdapterProperties properties() {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        Map<String, java.util.List<String>> mappings = new LinkedHashMap<String, java.util.List<String>>();
        mappings.put("admin", Arrays.asList("BPI_ADMIN", "BPI_OPERATOR"));
        properties.setRoleMappings(mappings);
        BpiAdapterProperties.SubjectScope scope = new BpiAdapterProperties.SubjectScope();
        scope.setTenantIds(Collections.singletonList("1000"));
        scope.setPlantIds(Collections.singletonList("PLANT-01"));
        scope.setLineIds(Collections.singletonList("LINE-S07-01"));
        properties.setSubjectScopes(Collections.singletonMap("admin", scope));
        return properties;
    }

    private Jwt jwt(String username) {
        Map<String, Object> headers = Collections.<String, Object>singletonMap("alg", "RS256");
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "user-1");
        claims.put("userName", username);
        claims.put("companyId", 1000);
        claims.put("realm_access", Collections.singletonMap("roles", Arrays.asList("offline_access", "admin")));
        Instant now = Instant.parse("2026-07-12T09:00:00Z");
        return new Jwt("legacy-token", now, now.plusSeconds(300), headers, claims);
    }
}
