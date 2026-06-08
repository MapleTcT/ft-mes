/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONObject
 *  io.jsonwebtoken.ExpiredJwtException
 *  io.jsonwebtoken.UnsupportedJwtException
 *  org.apache.commons.codec.digest.HmacAlgorithms
 *  org.apache.commons.codec.digest.HmacUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.util.MultiValueMap
 *  org.springframework.util.StringUtils
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supos.suposgateway.SuposGatewayConstants;
import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.feign.client.UserAdminClient;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import com.supcon.supos.suposgateway.repository.AuthTicketDao;
import com.supcon.supos.suposgateway.repository.SecretKeyDao;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import com.supcon.supos.suposgateway.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Resource;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class JwtAuthenticationTokenFilter
implements GlobalFilter,
Ordered,
ILogger,
InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);
    @Resource
    private AuthTicketDao authTicketDao;
    @Resource
    private PathMatcher pathMatcher;
    @Autowired
    private SecretKeyDao secretKeyDao;
    @Value(value="${oauth.secret}")
    private String secret;
    private PublicKey publicKey;
    @Resource
    private UserAdminClient userAdminClient;

    public void afterPropertiesSet() throws Exception {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(this.secret));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = keyFactory.generatePublic(pubKeySpec);
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (this.pathMatcher.matchBlackList(exchange)) {
            throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, "Black list");
        }
        if (this.pathMatcher.matchExcludeGlobal(exchange)) {
            return chain.filter(exchange);
        }
        String authHeaderValue = exchange.getRequest().getHeaders().getFirst("Authorization");
        String cookie = exchange.getRequest().getHeaders().getFirst("Cookie");
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (!org.apache.commons.lang3.StringUtils.isEmpty((CharSequence)authHeaderValue) && authHeaderValue.startsWith("Sign")) {
            return this.checkSign(authHeaderValue, exchange, chain);
        }
        if (!org.apache.commons.lang3.StringUtils.isEmpty((CharSequence)authHeaderValue) && authHeaderValue.startsWith("Bearer")) {
            if (rawPath.contains("open-api") || rawPath.contains("openapi")) {
                String[] arr = authHeaderValue.split(" ");
                if (arr.length != 2) {
                    throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "lack of authorization info");
                }
                String str = arr[1];
                if (JwtUtil.isJwtFormat(str, this.publicKey)) {
                    return this.checkJwt(exchange, chain);
                }
                return this.checkTicket(exchange, chain);
            }
            return this.checkTicket(exchange, chain);
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty((CharSequence)cookie)) {
            return this.checkTicket(exchange, chain);
        }
        throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign is null and Bearer is null");
    }

    private Mono<Void> checkJwt(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            String authHeaderValue = exchange.getRequest().getHeaders().getFirst("Authorization");
            assert (authHeaderValue != null);
            String[] arr = authHeaderValue.split(" ");
            if (arr.length != 2) {
                throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "lack of authorization info");
            }
            String jwt = arr[1];
            JwtUtil.validateJwt(jwt, this.publicKey);
        }
        catch (ExpiredJwtException e) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Jwt is expired");
        }
        catch (UnsupportedJwtException e) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Jwt is invalid");
        }
        catch (Exception e) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Jwt error");
        }
        return chain.filter(exchange);
    }

    private Mono<Void> checkTicket(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ticket = JwtUtil.parseTokenTicket(exchange.getRequest());
        if (org.apache.commons.lang3.StringUtils.isBlank((CharSequence)ticket)) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket is null");
        }
        Map<Object, Object> authorizationMap = this.authTicketDao.getMapByTicket(ticket);
        if (authorizationMap.isEmpty()) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket is expired");
        }
        String username = (String)authorizationMap.get("userName");
        String accessToken = (String)authorizationMap.get("access_token");
        ServerHttpRequest transferRequest = exchange.getRequest().mutate().headers(httpHeaders -> {
            httpHeaders.remove((Object)"Authorization");
            httpHeaders.add("Authorization", "Bearer " + accessToken);
            httpHeaders.remove((Object)"jwt");
            httpHeaders.add("jwt", accessToken);
            httpHeaders.remove((Object)"X-User-Name");
            httpHeaders.add("X-User-Name", username);
        }).build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(transferRequest).build();
        if (org.apache.commons.lang3.StringUtils.isNotEmpty((CharSequence)accessToken)) {
            String payload = JwtUtil.parsePayload(accessToken);
            JSONObject userInfo = JSON.parseObject((String)payload);
            Long companyId = userInfo.getLong("company_id");
            Long userId = userInfo.getLong("user_id");
            mutatedExchange.getAttributes().putIfAbsent(SuposGatewayConstants.USERID, userId);
            mutatedExchange.getAttributes().putIfAbsent(SuposGatewayConstants.COMPANYID, companyId);
        }
        return chain.filter(mutatedExchange);
    }

    private Mono<Void> checkSign(String authHeaderValue, ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (this.pathMatcher.matchExcludeSign(exchange)) {
            return chain.filter(exchange);
        }
        String tmp = authHeaderValue.replaceFirst("Sign", "").trim();
        String[] sp = tmp.split("-");
        String accessKey = sp[0];
        String sign = sp[1];
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        String secretKey = this.secretKeyDao.get(tenantId, accessKey);
        if (StringUtils.isEmpty((Object)secretKey)) {
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "secret key is null");
        }
        HttpHeaders headers = request.getHeaders();
        String method = request.getMethodValue().toUpperCase();
        String uri = request.getURI().getPath();
        String contentType = headers.getContentType() == null ? "" : headers.getContentType().toString();
        String canonicalQueryString = this.getCanonicalQueryString(request);
        String canonicalCustomHeaders = this.getCanonicalCustomHeaders(request);
        String bodyPayload = this.getBodyPayload(exchange);
        StringBuilder sb = new StringBuilder();
        sb.append(method).append("\n").append(uri).append("\n").append(contentType).append("\n").append(canonicalQueryString).append("\n").append(canonicalCustomHeaders).append("\n").append(bodyPayload);
        HmacUtils hmacSha256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey);
        String genSign = hmacSha256.hmacHex(sb.toString());
        if (!genSign.equals(sign)) {
            HttpUtils.logWarn((ILogger)this, exchange, "\r\nSign check failed.\r\n--------------Plaintext--------------\r\n{}\r\n--------------Sign--------------\r\n{}\r\n--------------GenSign--------------\r\n{}", sb.toString(), sign, genSign);
            throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign check failed.");
        }
        return chain.filter(exchange);
    }

    private String getCanonicalQueryString(ServerHttpRequest request) {
        MultiValueMap queryParams = request.getQueryParams();
        if (queryParams.isEmpty()) {
            return "";
        }
        TreeMap<String, String> params = new TreeMap<String, String>();
        for (Map.Entry entry : queryParams.entrySet()) {
            String key = ((String)entry.getKey()).toLowerCase();
            List values = (List)entry.getValue();
            StringBuilder value = new StringBuilder((String)values.get(0));
            if (values.size() > 1) {
                for (int i = 1; i < values.size(); ++i) {
                    value.append(",").append((String)values.get(i));
                }
            }
            params.put(key, value.toString());
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append((String)params.get(key));
        }
        return sb.toString();
    }

    private String getCanonicalCustomHeaders(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        if (headers.isEmpty()) {
            return "";
        }
        TreeMap<String, String> params = new TreeMap<String, String>();
        for (Map.Entry entry : headers.entrySet()) {
            String key = ((String)entry.getKey()).toLowerCase();
            if (!key.startsWith("x-mc-")) continue;
            String value = ((String)((List)entry.getValue()).get(0)).trim();
            params.put(key, value);
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(key).append(":").append((String)params.get(key));
        }
        return sb.toString();
    }

    private String getBodyPayload(ServerWebExchange exchange) {
        return (String)exchange.getAttributeOrDefault(SuposGatewayConstants.CACHED_REQUEST_BODY, (Object)"");
    }

    public int getOrder() {
        return FilterOrder.AUTH.getOrder();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public static void main(String[] args) {
    }
}

