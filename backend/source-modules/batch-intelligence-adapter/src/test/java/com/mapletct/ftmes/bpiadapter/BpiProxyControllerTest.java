package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class BpiProxyControllerTest {

    @Test
    public void replacesLegacyTokenAndForwardsCommandContractToFixedUpstream() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        String id = "9c392d57-7502-4cd8-bc37-e72961bb08b4";
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/candidates/" + id + "/confirm"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "command-1"))
                .andExpect(header("If-Match", "1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION,
                        allOf(startsWith("Bearer "), not(startsWith("Bearer legacy-token")))))
                .andRespond(withSuccess("{\"code\":200}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bpi-api/candidates/" + id + "/confirm");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        request.addHeader("Idempotency-Key", "command-1");
        request.addHeader("If-Match", "1");
        request.addHeader("X-Tenant-Id", "attacker-tenant");

        ResponseEntity<byte[]> response = controller.proxy(jwt(), request, "{\"reason\":\"operator confirmed\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        upstream.verify();
    }

    private BpiAdapterProperties properties() {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        properties.setUpstreamBaseUrl("http://bpi-service:19091");
        properties.setInternalJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setInternalTokenTtl(Duration.ofMinutes(10));
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

    private Jwt jwt() {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "user-1");
        claims.put("userName", "admin");
        claims.put("companyId", 1000);
        claims.put("realm_access", Collections.singletonMap("roles", Collections.singletonList("admin")));
        Instant now = Instant.parse("2026-07-12T09:00:00Z");
        return new Jwt("legacy-token", now, now.plusSeconds(300),
                Collections.<String, Object>singletonMap("alg", "RS256"), claims);
    }
}
