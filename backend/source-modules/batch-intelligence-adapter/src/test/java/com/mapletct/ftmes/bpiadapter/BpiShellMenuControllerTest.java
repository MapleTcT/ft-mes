package com.mapletct.ftmes.bpiadapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class BpiShellMenuControllerTest {

    private static final String GATEWAY_MENU_URL =
            "http://gateway:8008/inter-api/rbac/v1/menus/currentUser";
    private static final String FEATURE_FLAG_URL =
            "http://bpi-service:19091/bpi/v1/feature-flags"
                    + "?plantId=PLANT-01&lineId=LINE-S07-01&scopeType=LINE";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void injectsNativeShellMenuAfterProductionWhenUiFlagIsEnabled() throws Exception {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer legacy-token"))
                .andRespond(withSuccess(baseMenus(false), MediaType.APPLICATION_JSON));
        fixture.server.expect(requestTo(FEATURE_FLAG_URL))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION,
                        allOf(startsWith("Bearer "), not(startsWith("Bearer legacy-token")))))
                .andRespond(withSuccess(flags(true), MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("VISIBLE_INJECTED", response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        JsonNode menus = objectMapper.readTree(response.getBody()).path("list");
        assertEquals("makeTask", menus.get(0).path("code").asText());
        assertEquals("BPI", menus.get(1).path("code").asText());
        assertEquals("智能批次", menus.get(1).path("nameDisplay").asText());
        assertEquals("/bpi/#/overview", menus.get(1).path("children").get(0).path("url").asText());
        assertEquals("quality", menus.get(2).path("code").asText());
        fixture.server.verify();
    }

    @Test
    public void preservesExistingNativeBpiMenuWhenUiFlagIsEnabled() throws Exception {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(baseMenus(true), MediaType.APPLICATION_JSON));
        fixture.server.expect(requestTo(FEATURE_FLAG_URL))
                .andRespond(withSuccess(flags(true), MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals("VISIBLE_EXISTING",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        JsonNode menus = objectMapper.readTree(response.getBody()).path("list");
        assertEquals(3, menus.size());
        assertEquals("stale BPI", menus.get(2).path("nameDisplay").asText());
        fixture.server.verify();
    }

    @Test
    public void forwardsGatewayMenusUnchangedWhenShellBridgeIsDisabled() {
        Fixture fixture = fixture();
        fixture.properties.setShellMenuEnabled(false);
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(baseMenus(true), MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals("BYPASSED_DISABLED",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        assertEquals(baseMenus(true), new String(response.getBody(), StandardCharsets.UTF_8));
        fixture.server.verify();
    }

    @Test
    public void forwardsInvalidGatewayMenuContractWithoutCallingBpiService() {
        Fixture fixture = fixture();
        String invalid = "{\"data\":[]}";
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(invalid, MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals("BYPASSED_INVALID_MENU",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        assertEquals(invalid, new String(response.getBody(), StandardCharsets.UTF_8));
        fixture.server.verify();
    }

    @Test
    public void removesAnyExistingBpiMenuWhenUiFlagIsDisabled() throws Exception {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(baseMenus(true), MediaType.APPLICATION_JSON));
        fixture.server.expect(requestTo(FEATURE_FLAG_URL))
                .andRespond(withSuccess(flags(false), MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals("HIDDEN_DISABLED", response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        JsonNode menus = objectMapper.readTree(response.getBody()).path("list");
        assertEquals(2, menus.size());
        assertFalse(containsCode(menus, "BPI"));
        assertTrue(containsCode(menus, "makeTask"));
        assertTrue(containsCode(menus, "quality"));
        fixture.server.verify();
    }

    @Test
    public void hidesOnlyBpiAndPreservesBaseMenusWhenFlagServiceIsUnavailable() throws Exception {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(baseMenus(true), MediaType.APPLICATION_JSON));
        fixture.server.expect(requestTo(FEATURE_FLAG_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("HIDDEN_UPSTREAM_UNAVAILABLE",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        JsonNode menus = objectMapper.readTree(response.getBody()).path("list");
        assertFalse(containsCode(menus, "BPI"));
        assertTrue(containsCode(menus, "makeTask"));
        assertTrue(containsCode(menus, "quality"));
        fixture.server.verify();
    }

    @Test
    public void hidesBpiWithoutCallingServiceWhenSubjectHasNoServerScope() throws Exception {
        Fixture fixture = fixture();
        fixture.properties.setSubjectScopes(Collections.<String, BpiAdapterProperties.SubjectScope>emptyMap());
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withSuccess(baseMenus(true), MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("HIDDEN_SCOPE_DENIED",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        assertFalse(containsCode(objectMapper.readTree(response.getBody()).path("list"), "BPI"));
        fixture.server.verify();
    }

    @Test
    public void returnsGatewayAuthenticationFailureWithoutCallingBpiService() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(GATEWAY_MENU_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":401}")
                        .contentType(MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = fixture.controller.currentUserMenus(jwt(), request());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("BYPASSED_GATEWAY_STATUS",
                response.getHeaders().getFirst(BpiShellMenuController.UI_GATE_HEADER));
        assertEquals("{\"code\":401}", new String(response.getBody(), StandardCharsets.UTF_8));
        fixture.server.verify();
    }

    private Fixture fixture() {
        BpiAdapterProperties properties = properties();
        RestTemplate restTemplate = new AdapterConfiguration().bpiRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BpiShellMenuController controller = new BpiShellMenuController(
                properties,
                new BpiClaimsMapper(properties),
                new InternalJwtIssuer(properties),
                restTemplate,
                objectMapper);
        return new Fixture(properties, server, controller);
    }

    private BpiAdapterProperties properties() {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        properties.setUpstreamBaseUrl("http://bpi-service:19091");
        properties.setLegacyGatewayBaseUrl("http://gateway:8008");
        properties.setInternalJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setInternalTokenTtl(Duration.ofMinutes(10));
        properties.setShellMenuEnabled(true);
        properties.setShellPlantId("PLANT-01");
        properties.setShellLineId("LINE-S07-01");
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

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", BpiShellMenuController.SHELL_MENU_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy-token");
        request.addHeader("X-Tenant-Id", "dt");
        request.addHeader("X-Trace-Id", "shell-menu-test");
        return request;
    }

    private Jwt jwt() {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "user-1");
        claims.put("userName", "admin");
        claims.put("companyId", 1000);
        claims.put("realm_access", Collections.singletonMap("roles", Collections.singletonList("admin")));
        Instant now = Instant.parse("2026-07-20T04:00:00Z");
        return new Jwt("legacy-token", now, now.plusSeconds(300),
                Collections.<String, Object>singletonMap("alg", "RS256"), claims);
    }

    private String baseMenus(boolean includeBpi) {
        String bpi = includeBpi
                ? ",{\"id\":9,\"code\":\"BPI\",\"nameDisplay\":\"stale BPI\",\"cid\":1000,\"children\":[]}" : "";
        return "{\"list\":["
                + "{\"id\":1,\"code\":\"makeTask\",\"nameDisplay\":\"生产管理\",\"cid\":1000,\"children\":[]},"
                + "{\"id\":2,\"code\":\"quality\",\"nameDisplay\":\"质量管理\",\"cid\":1000,\"children\":[]}"
                + bpi + "]}";
    }

    private String flags(boolean enabled) {
        return "{\"data\":[{\"flagKey\":\"bpi.ui\",\"effectiveEnabled\":" + enabled + "}]}";
    }

    private boolean containsCode(JsonNode menus, String code) {
        for (JsonNode menu : menus) {
            if (code.equals(menu.path("code").asText())) return true;
        }
        return false;
    }

    private static final class Fixture {
        private final BpiAdapterProperties properties;
        private final MockRestServiceServer server;
        private final BpiShellMenuController controller;

        private Fixture(
                BpiAdapterProperties properties,
                MockRestServiceServer server,
                BpiShellMenuController controller) {
            this.properties = properties;
            this.server = server;
            this.controller = controller;
        }
    }
}
