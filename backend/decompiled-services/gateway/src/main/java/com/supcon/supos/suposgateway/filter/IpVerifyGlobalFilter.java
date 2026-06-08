/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONObject
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.http.HttpStatus
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.feign.client.UserAdminClient;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import com.supcon.supos.suposgateway.repository.AuthTicketDao;
import com.supcon.supos.suposgateway.utils.ILogger;
import com.supcon.supos.suposgateway.utils.JwtUtil;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class IpVerifyGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpVerifyGlobalFilter.class);
    private static final String ASTERISK_PATTERN = "(1\\\\d{2}|2[0-4]\\\\d|25[0-5]|[1-9]\\\\d|[1-9])";
    private static final String QUESTION_MARK_PATTERN = "([0-9])";
    @Resource
    private AuthTicketDao authTicketDao;
    @Resource
    private PathMatcher pathMatcher;
    @Resource
    private UserAdminClient userAdminClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (!rawPath.contains("open-api") && !rawPath.contains("openapi")) {
            if (this.pathMatcher.matchExcludeGlobal(exchange)) {
                return chain.filter(exchange);
            }
            String ticket = JwtUtil.parseTokenTicket(exchange.getRequest());
            if (StringUtils.isBlank((CharSequence)ticket)) {
                throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket is null");
            }
            Map<Object, Object> authorizationMap = this.authTicketDao.getMapByTicket(ticket);
            if (authorizationMap.isEmpty()) {
                throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket is expired");
            }
            String companyId = (String)authorizationMap.get("companyId");
            if (!StringUtils.isEmpty((CharSequence)companyId)) {
                String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
                String ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
                boolean isLegal = this.isLegal(Long.valueOf(companyId), tenantId, ip);
                if (!isLegal) {
                    this.userAdminClient.removeOnlineUserByTicket(ticket, tenantId);
                    throw new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, "IP Blacklist");
                }
            }
        }
        return chain.filter(exchange);
    }

    private boolean isLegal(Long companyId, String tenantId, String ip) {
        boolean isLegal = false;
        String tenantType = String.format("AUTH:IWB:CID:CONTROL-TYPE:TENANTID:%s", tenantId);
        String controlTypeStr = (String)this.stringRedisTemplate.opsForHash().get((Object)tenantType, (Object)companyId.toString());
        Integer controlType = Optional.ofNullable(controlTypeStr).map(Integer::valueOf).orElse(null);
        if (controlType == null) {
            isLegal = true;
        } else {
            String key = String.format("AUTH:IWB:TENANTID:%s:CID:%d", tenantId, companyId);
            Set ipList = this.stringRedisTemplate.opsForSet().members((Object)key);
            HashSet<String> controlIpList = new HashSet<String>();
            for (String originIp : ipList) {
                if (originIp.contains("*")) {
                    originIp = originIp.replaceAll("\\*", ASTERISK_PATTERN);
                }
                if (originIp.contains("?")) {
                    originIp = originIp.replaceAll("\\?", QUESTION_MARK_PATTERN);
                }
                controlIpList.add(originIp);
            }
            boolean isContains = false;
            for (String controlIp : controlIpList) {
                if (!ip.matches(controlIp)) continue;
                isContains = true;
                break;
            }
            isLegal = controlType == 0 && !isContains || controlType == 1 && isContains;
        }
        return isLegal;
    }

    public int getOrder() {
        return FilterOrder.IP_VERIFY.getOrder();
    }

    private boolean verifyIpBlackWhiteList(String ip, Long companyId, String tenantId) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("companyId", (Object)companyId);
        requestBody.put("ip", (Object)ip);
        JSONObject responseBody = JSON.parseObject((String)this.userAdminClient.verifyIp(requestBody, tenantId));
        return responseBody.getBoolean("data");
    }
}

