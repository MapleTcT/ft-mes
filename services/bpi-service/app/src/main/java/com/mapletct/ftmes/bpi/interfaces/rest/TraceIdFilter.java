package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requested = request.getHeader("X-Trace-Id");
        String traceId = requested != null && SAFE_TRACE_ID.matcher(requested).matches()
                ? requested
                : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, traceId);
        response.setHeader("X-Trace-Id", traceId);
        filterChain.doFilter(request, response);
    }
}
