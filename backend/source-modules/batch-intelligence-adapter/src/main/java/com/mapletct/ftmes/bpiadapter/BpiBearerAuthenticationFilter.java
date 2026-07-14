package com.mapletct.ftmes.bpiadapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class BpiBearerAuthenticationFilter extends OncePerRequestFilter {

    private final BpiCredentialAuthenticator authenticator;
    private final ObjectMapper objectMapper;

    public BpiBearerAuthenticationFilter(BpiCredentialAuthenticator authenticator, ObjectMapper objectMapper) {
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String credential = resolveCredential(request);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    authenticator.authenticate(credential), Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException error) {
            SecurityContextHolder.clearContext();
            unauthorized(request, response, error);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveCredential(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String value = authorization.substring(7).trim();
            if (!value.isEmpty()) return value;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("suposTicket".equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().trim().isEmpty()) return cookie.getValue().trim();
            }
        }
        throw new BadCredentialsException("BPI bearer token or suposTicket cookie is required");
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, RuntimeException error)
            throws IOException {
        Map<String, Object> problem = new LinkedHashMap<String, Object>();
        problem.put("type", "urn:ft-mes:bpi:authentication-failed");
        problem.put("title", "BPI authentication failed");
        problem.put("status", 401);
        problem.put("detail", error.getMessage() == null ? "Credential validation failed" : error.getMessage());
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());
        problem.put("traceId", UUID.randomUUID().toString());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
