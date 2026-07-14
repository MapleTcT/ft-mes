package com.mapletct.ftmes.bpiadapter;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LegacyTicketVerifier {

    private static final Pattern TICKET = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<Map<String, Object>>() { };

    private final BpiAdapterProperties properties;
    private final RestTemplate restTemplate;
    private final Clock clock;

    @Autowired
    public LegacyTicketVerifier(BpiAdapterProperties properties, RestTemplate restTemplate) {
        this(properties, restTemplate, Clock.systemUTC());
    }

    LegacyTicketVerifier(BpiAdapterProperties properties, RestTemplate restTemplate, Clock clock) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    public boolean supports(String credential) {
        return properties.isLegacyTicketEnabled() && credential != null && TICKET.matcher(credential).matches();
    }

    public Jwt verify(String ticket) {
        if (!supports(ticket)) throw new BadCredentialsException("Legacy ADP ticket format is invalid");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ticket);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        String endpoint = trimSlash(properties.getLegacyGatewayBaseUrl()) + "/inter-api/auth/v1/currentuser";
        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.GET, new HttpEntity<Void>(headers), MAP);
        } catch (RestClientException error) {
            throw new BadCredentialsException("Legacy ADP ticket verification request failed", error);
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BadCredentialsException("Legacy ADP ticket was rejected by the trusted gateway");
        }

        Map<String, Object> body = response.getBody();
        Map<?, ?> userInfo = body == null ? null : asMap(body.get("userInfo"));
        String username = text(userInfo == null ? null : userInfo.get("username"));
        String tenantId = text(userInfo == null ? null : userInfo.get("cid"));
        if (username == null || tenantId == null) {
            throw new BadCredentialsException("Trusted gateway returned incomplete legacy identity data");
        }

        List<String> roles = new ArrayList<String>();
        Object rawRoles = userInfo.get("userRoleList");
        if (rawRoles instanceof Iterable) {
            for (Object rawRole : (Iterable<?>) rawRoles) {
                Map<?, ?> role = asMap(rawRole);
                if (role == null) continue;
                addRole(roles, role.get("code"));
                addRole(roles, role.get("name"));
                addRole(roles, role.get("showName"));
            }
        }
        if (roles.isEmpty()) {
            throw new BadCredentialsException("Trusted gateway returned no legacy roles");
        }

        Instant issuedAt = clock.instant();
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "legacy-ticket:" + username);
        claims.put(properties.getUsernameClaim(), username);
        claims.put(properties.getTenantClaim(), tenantId);
        claims.put("roles", roles);
        return new Jwt("verified-legacy-ticket", issuedAt, issuedAt.plusSeconds(60),
                Collections.<String, Object>singletonMap("alg", "legacy-ticket"), claims);
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private void addRole(List<String> roles, Object value) {
        String role = text(value);
        if (role != null && !roles.contains(role)) roles.add(role);
    }

    private String text(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
