package com.mapletct.ftmes.bpiadapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BpiBearerAuthenticationFilterTest {

    private static final String TICKET = "11111111-2222-3333-4444-555555555555";

    @Test
    public void authenticatesHttpOnlyLegacyCookieAndClearsContextAfterRequest() throws Exception {
        BpiCredentialAuthenticator authenticator = mock(BpiCredentialAuthenticator.class);
        Jwt jwt = jwt();
        when(authenticator.authenticate(TICKET)).thenReturn(jwt);
        BpiBearerAuthenticationFilter filter = new BpiBearerAuthenticationFilter(authenticator, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bpi-api/overview");
        request.setCookies(new Cookie("suposTicket", TICKET));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> observed = new AtomicReference<Authentication>();
        FilterChain chain = (req, res) -> observed.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        verify(authenticator).authenticate(TICKET);
        assertSame(jwt, observed.get().getPrincipal());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void rejectsRequestWithoutBearerOrLegacyCookie() throws Exception {
        BpiCredentialAuthenticator authenticator = mock(BpiCredentialAuthenticator.class);
        BpiBearerAuthenticationFilter filter = new BpiBearerAuthenticationFilter(authenticator, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bpi-api/overview");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("unauthenticated request reached the controller chain");
        });

        assertEquals(401, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    private Jwt jwt() {
        Instant now = Instant.parse("2026-07-14T08:00:00Z");
        return new Jwt("credential", now, now.plusSeconds(60),
                Collections.<String, Object>singletonMap("alg", "test"),
                Collections.<String, Object>singletonMap("sub", "legacy-ticket:admin"));
    }
}
