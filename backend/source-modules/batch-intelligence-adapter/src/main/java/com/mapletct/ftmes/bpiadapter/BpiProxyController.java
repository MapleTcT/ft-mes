package com.mapletct.ftmes.bpiadapter;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class BpiProxyController {

    private static final String PREFIX = "/bpi-api";
    private static final int DEFAULT_BODY_LIMIT = 65_536;
    private static final int POINT_CATALOG_BODY_LIMIT = 5 * 1024 * 1024;
    private static final Pattern TRACE = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final List<String> FORWARDED_REQUEST_HEADERS = Arrays.asList(
            HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE, "Idempotency-Key", "If-Match", "X-Trace-Id");
    private static final List<String> FORWARDED_RESPONSE_HEADERS = Arrays.asList(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.CACHE_CONTROL, HttpHeaders.ETAG, "Idempotent-Replay", "X-Trace-Id", "Retry-After");

    private final BpiAdapterProperties properties;
    private final BpiClaimsMapper claimsMapper;
    private final InternalJwtIssuer jwtIssuer;
    private final BpiRoutePolicy routePolicy;
    private final RestTemplate restTemplate;

    public BpiProxyController(BpiAdapterProperties properties, BpiClaimsMapper claimsMapper,
                              InternalJwtIssuer jwtIssuer, BpiRoutePolicy routePolicy, RestTemplate restTemplate) {
        this.properties = properties;
        this.claimsMapper = claimsMapper;
        this.jwtIssuer = jwtIssuer;
        this.routePolicy = routePolicy;
        this.restTemplate = restTemplate;
    }

    @RequestMapping(PREFIX + "/**")
    public ResponseEntity<byte[]> proxy(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String requestUri = request.getRequestURI();
        String path = requestUri.startsWith(PREFIX) ? requestUri.substring(PREFIX.length()) : "";
        if (method == null || !routePolicy.allows(method, path)) {
            throw new AdapterAccessDeniedException("BPI route is not allowlisted");
        }

        BpiActor actor = claimsMapper.map(jwt);
        HttpHeaders upstreamHeaders = new HttpHeaders();
        for (String name : FORWARDED_REQUEST_HEADERS) {
            String value = request.getHeader(name);
            if (StringUtils.hasText(value)) upstreamHeaders.set(name, value);
        }
        String traceId = upstreamHeaders.getFirst("X-Trace-Id");
        if (traceId == null || !TRACE.matcher(traceId).matches()) traceId = UUID.randomUUID().toString();
        upstreamHeaders.set("X-Trace-Id", traceId);
        upstreamHeaders.setBearerAuth(jwtIssuer.issue(actor));
        int bodyLimit = "/point-catalog/snapshots".equals(path)
                ? POINT_CATALOG_BODY_LIMIT : DEFAULT_BODY_LIMIT;
        if (body != null && body.length > bodyLimit) {
            throw new AdapterAccessDeniedException(
                    "/point-catalog/snapshots".equals(path)
                            ? "BPI point catalog request body exceeds 5 MiB"
                            : "BPI request body exceeds 64 KiB");
        }

        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(trimSlash(properties.getUpstreamBaseUrl()))
                .path("/bpi/v1").path(path);
        if (StringUtils.hasText(request.getQueryString())) uri.query(request.getQueryString());
        URI target = uri.build(true).toUri();
        ResponseEntity<byte[]> upstream = restTemplate.exchange(target, method,
                new HttpEntity<byte[]>(body == null ? new byte[0] : body, upstreamHeaders), byte[].class);

        HttpHeaders responseHeaders = new HttpHeaders();
        for (String name : FORWARDED_RESPONSE_HEADERS) {
            if (upstream.getHeaders().containsKey(name)) responseHeaders.put(name, upstream.getHeaders().get(name));
        }
        if (!responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        responseHeaders.set("X-Trace-Id", traceId);
        return new ResponseEntity<byte[]>(upstream.getBody(), responseHeaders, upstream.getStatusCode());
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
