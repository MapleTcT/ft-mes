package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

public class LegacyTicketVerifierTest {

    private static final String TICKET = "11111111-2222-3333-4444-555555555555";

    @Test
    public void verifiesOpaqueTicketThroughTrustedGatewayAndBuildsScopedClaims() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://gateway:8008/inter-api/auth/v1/currentuser"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TICKET))
                .andRespond(withSuccess("{\"userInfo\":{\"cid\":1000,\"username\":\"admin\","
                        + "\"userRoleList\":[{\"code\":\"systemRole\",\"name\":\"manager\"}]}}",
                        MediaType.APPLICATION_JSON));

        LegacyTicketVerifier verifier = new LegacyTicketVerifier(properties(), restTemplate,
                Clock.fixed(Instant.parse("2026-07-14T08:00:00Z"), ZoneOffset.UTC));
        Jwt jwt = verifier.verify(TICKET);

        assertEquals("legacy-ticket:admin", jwt.getSubject());
        assertEquals("admin", jwt.getClaimAsString("userName"));
        assertEquals("1000", jwt.getClaimAsString("companyId"));
        assertTrue(jwt.getClaimAsStringList("roles").contains("systemRole"));
        server.verify();
    }

    @Test(expected = BadCredentialsException.class)
    public void rejectsTicketWhenTrustedGatewayRejectsIt() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://gateway:8008/inter-api/auth/v1/currentuser"))
                .andRespond(withUnauthorizedRequest());
        new LegacyTicketVerifier(properties(), restTemplate).verify(TICKET);
    }

    private BpiAdapterProperties properties() {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        properties.setLegacyTicketEnabled(true);
        properties.setLegacyGatewayBaseUrl("http://gateway:8008");
        return properties;
    }
}
