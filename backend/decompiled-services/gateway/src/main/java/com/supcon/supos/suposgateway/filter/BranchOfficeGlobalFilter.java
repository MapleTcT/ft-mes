/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.http.HttpCookie
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpMethod
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.http.server.reactive.ServerHttpResponse
 *  org.springframework.util.MultiValueMap
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.feign.client.BranchOfficeApiClient;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import com.supcon.supos.suposgateway.repository.AuthTicketDao;
import com.supcon.supos.suposgateway.utils.Base64Util;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class BranchOfficeGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchOfficeGlobalFilter.class);
    @Resource
    private AuthTicketDao authTicketDao;
    @Resource
    private PathMatcher pathMatcher;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private BranchOfficeApiClient branchOfficeApiClient;

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String rawPath = uri.getRawPath();
        String uriPath = uri.getPath();
        boolean isBranchOfficeLogin = uriPath.startsWith("/inter-api/auth/v1/branch-office/authorize/login");
        if (!isBranchOfficeLogin && (rawPath.contains("open-api") || rawPath.contains("openapi") || this.pathMatcher.matchExcludeGlobal(exchange))) {
            return chain.filter(exchange);
        }
        String ticket = BranchOfficeGlobalFilter.parseTokenTicket(request);
        boolean needAuthentication = this.needAuthentication(ticket);
        if (!needAuthentication) {
            if (request.getHeaders().getFirst("Authorization") == null) {
                ServerHttpRequest newRequest = exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.add("Authorization", "Bearer " + ticket)).build();
                return chain.filter(exchange.mutate().request(newRequest).build());
            }
            return chain.filter(exchange);
        }
        if (this.isXHRequest(request.getHeaders()) || rawPath.contains("/inter-api/i18n/v1/resource/code/all/module_ids/allkeyvalues")) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket is expired");
        }
        String path = uri.toString();
        String hostUrl = path.substring(0, path.indexOf("/", path.indexOf("//") + 2));
        String originUrl = null;
        if (isBranchOfficeLogin) {
            MultiValueMap queryParams = request.getQueryParams();
            String redirectUrl = (String)queryParams.getFirst((Object)"redirectUrl");
            originUrl = hostUrl + (StringUtils.isNotEmpty((CharSequence)redirectUrl) && !"null".equals(redirectUrl) ? redirectUrl : "");
        } else {
            originUrl = HttpMethod.GET.equals((Object)request.getMethod()) ? path : hostUrl;
        }
        String authorizeUrl = this.branchOfficeApiClient.authorizeUrl(Base64Util.encode(originUrl), Base64Util.encode(hostUrl));
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().set("Location", authorizeUrl);
        return response.setComplete();
    }

    private boolean isXHRequest(HttpHeaders headers) {
        List values = headers.get((Object)"X-Requested-With");
        if (values == null || values.isEmpty()) {
            return false;
        }
        String value = (String)values.get(0);
        return "XMLHttpRequest".equals(value);
    }

    private boolean needAuthentication(String ticket) {
        if (StringUtils.isBlank((CharSequence)ticket)) {
            return true;
        }
        Map<Object, Object> authorizationMap = this.authTicketDao.getMapByTicket(ticket);
        return authorizationMap.isEmpty();
    }

    public static String parseTokenTicket(ServerHttpRequest request) {
        HttpCookie cookie;
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank((CharSequence)token) && (cookie = (HttpCookie)request.getCookies().getFirst((Object)"suposTicket")) != null) {
            token = cookie.getValue();
        }
        if (StringUtils.isBlank((CharSequence)token) && HttpUtils.isWS(request)) {
            token = (String)request.getQueryParams().getFirst((Object)"token");
        }
        if (StringUtils.isBlank((CharSequence)token)) {
            return null;
        }
        if (token.startsWith("Bearer")) {
            token = token.split(" ")[1];
        }
        return token;
    }

    public int getOrder() {
        return FilterOrder.BRANCH_OFFICE.getOrder();
    }
}

