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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

    @Test
    public void forwardsCandidateRejectionWithConcurrencyHeaders() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        String id = "9c392d57-7502-4cd8-bc37-e72961bb08b4";
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/candidates/" + id + "/reject"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "reject-command-1"))
                .andExpect(header("If-Match", "4"))
                .andRespond(withSuccess("{\"code\":200}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/candidates/" + id + "/reject");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        request.addHeader("Idempotency-Key", "reject-command-1");
        request.addHeader("If-Match", "4");

        ResponseEntity<byte[]> response = controller.proxy(jwt(), request,
                "{\"reason\":\"operator rejected\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        upstream.verify();
    }

    @Test
    public void forwardsBatchSuspendAndResumeOnlyThroughTheFixedUpstream() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        String id = "9c392d57-7502-4cd8-bc37-e72961bb08b4";
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/batches/" + id + "/suspend"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "suspend-command-1"))
                .andExpect(header("If-Match", "1"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"state\":\"SUSPENDED\"}}", MediaType.APPLICATION_JSON));
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/batches/" + id + "/resume"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "resume-command-1"))
                .andExpect(header("If-Match", "2"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"state\":\"ACTIVE\"}}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest suspend = new MockHttpServletRequest(
                "POST", "/bpi-api/batches/" + id + "/suspend");
        suspend.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        suspend.addHeader("Idempotency-Key", "suspend-command-1");
        suspend.addHeader("If-Match", "1");
        assertEquals(HttpStatus.OK, controller.proxy(jwt(), suspend,
                "{\"reason\":\"context stale\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getStatusCode());

        MockHttpServletRequest resume = new MockHttpServletRequest(
                "POST", "/bpi-api/batches/" + id + "/resume");
        resume.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        resume.addHeader("Idempotency-Key", "resume-command-1");
        resume.addHeader("If-Match", "2");
        assertEquals(HttpStatus.OK, controller.proxy(jwt(), resume,
                "{\"reason\":\"context restored\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getStatusCode());

        upstream.verify();
    }

    @Test
    public void rejectsOrdinaryRequestBodyAbove64KiB() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/candidates/9c392d57-7502-4cd8-bc37-e72961bb08b4/confirm");

        try {
            controller.proxy(jwt(), request, new byte[65_537]);
            fail("Expected the ordinary request body limit to reject the request");
        } catch (AdapterAccessDeniedException error) {
            assertTrue(error.getMessage().contains("64 KiB"));
        }
    }

    @Test
    public void acceptsPointCatalogSnapshotAboveOrdinaryLimit() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/point-catalog/snapshots"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "catalog-import-1"))
                .andExpect(header("If-Match", "0"))
                .andRespond(withSuccess("{\"code\":201}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/point-catalog/snapshots");
        request.addHeader("Idempotency-Key", "catalog-import-1");
        request.addHeader("If-Match", "0");

        ResponseEntity<byte[]> response = controller.proxy(jwt(), request, new byte[65_537]);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        upstream.verify();
    }

    @Test
    public void forwardsPointCalibrationApprovalWithConcurrencyHeaders() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        String id = "9c392d57-7502-4cd8-bc37-e72961bb08b4";
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/point-calibrations/" + id + "/approve"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "calibration-approve-1"))
                .andExpect(header("If-Match", "1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION,
                        allOf(startsWith("Bearer "), not(startsWith("Bearer legacy-token")))))
                .andRespond(withSuccess("{\"data\":{\"state\":\"APPROVED\"}}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/point-calibrations/" + id + "/approve");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        request.addHeader("Idempotency-Key", "calibration-approve-1");
        request.addHeader("If-Match", "1");

        ResponseEntity<byte[]> response = controller.proxy(jwt(), request,
                "{\"reason\":\"独立复核证书后批准\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        upstream.verify();
    }

    @Test
    public void forwardsDataQualityAcknowledgementWithInternalIdentityAndConcurrencyHeaders() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer upstream = MockRestServiceServer.bindTo(restTemplate).build();
        String id = "9c392d57-7502-4cd8-bc37-e72961bb08b4";
        upstream.expect(requestTo("http://bpi-service:19091/bpi/v1/data-quality/incidents/" + id + "/acknowledge"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "data-quality-ack-1"))
                .andExpect(header("If-Match", "2"))
                .andExpect(header(HttpHeaders.AUTHORIZATION,
                        allOf(startsWith("Bearer "), not(startsWith("Bearer legacy-token")))))
                .andRespond(withSuccess("{\"data\":{\"state\":\"ACKNOWLEDGED\"}}", MediaType.APPLICATION_JSON));

        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/data-quality/incidents/" + id + "/acknowledge");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        request.addHeader("Idempotency-Key", "data-quality-ack-1");
        request.addHeader("If-Match", "2");

        ResponseEntity<byte[]> response = controller.proxy(jwt(), request,
                "{\"assignee\":\"shift.lead\",\"reason\":\"确认并分派事件\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        upstream.verify();
    }

    @Test
    public void rejectsPointCatalogSnapshotAbove5MiB() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        BpiProxyController controller = new BpiProxyController(properties, new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties), new BpiRoutePolicy(), restTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bpi-api/point-catalog/snapshots");

        try {
            controller.proxy(jwt(), request, new byte[(5 * 1024 * 1024) + 1]);
            fail("Expected the point catalog request body limit to reject the request");
        } catch (AdapterAccessDeniedException error) {
            assertTrue(error.getMessage().contains("5 MiB"));
        }
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
