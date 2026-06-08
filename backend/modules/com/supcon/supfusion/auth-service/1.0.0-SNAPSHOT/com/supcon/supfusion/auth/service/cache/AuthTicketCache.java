package com.supcon.supfusion.auth.service.cache;

import com.supcon.supfusion.auth.service.bo.AuthorizationBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.supcon.supfusion.auth.common.constants.Constants.*;


/**
 * @author caokele
 */
@Repository
@Slf4j
public class AuthTicketCache {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据ticket获取授权信息
     */
    public Map<Object, Object> getMapByTicket(String ticket) {
        String key = String.format(AUTH_TICKET, ticket);
        return stringRedisTemplate.opsForHash().entries(key);
    }

    /**
     * 根据ticket获取授权信息
     */
    public Long getExpire(String ticket) {
        String key = String.format(AUTH_TICKET, ticket);
        return stringRedisTemplate.getExpire(key,TimeUnit.SECONDS);
    }
    /**
     * 根据ticket删除授权信息
     */
    public void deleteByTicket(String ticket) {
        String key = String.format(AUTH_TICKET, ticket);
        stringRedisTemplate.delete(key);
    }

    /**
     * 持久化授权信息
     *
     * @param ticket          凭证
     * @param authorizationBO 认证信息
     */
    public void storeAuthorization(String ticket, AuthorizationBO authorizationBO) {
        Map<String, String> authorizationMap = new HashMap<>();
        authorizationMap.put(ACCESS_TOKEN, authorizationBO.getAccessToken());
        authorizationMap.put(REFRESH_TOKEN, authorizationBO.getRefreshToken());
        authorizationMap.put(TOKEN_TYPE, authorizationBO.getTokenType());
        authorizationMap.put(TENANT_ID, authorizationBO.getTenantId());
        authorizationMap.put(CLIENT_ID, authorizationBO.getClientId());
        authorizationMap.put(USER_NAME, authorizationBO.getUserName());
        if (authorizationBO.getCompanyId() != null) {
            authorizationMap.put(COMPANY_ID, authorizationBO.getCompanyId().toString());
        }

        authorizationMap.put(LOGIN_TYPE, authorizationBO.getLoginType());
        if ("1".equals(authorizationBO.getLoginType())) {
            authorizationMap.put(THIRD_TOKEN, authorizationBO.getClientAccessToken());
            authorizationMap.put(THIRD_REFRESH_TOKEN, authorizationBO.getClientRefreshToken());
            if(StringUtils.isNotEmpty(authorizationBO.getProtocolType())){
                authorizationMap.put(PROTOCOL_TYPE, authorizationBO.getProtocolType());
            }
        }

        String key = String.format(AUTH_TICKET, ticket);
        stringRedisTemplate.opsForHash().putAll(key, authorizationMap);
        stringRedisTemplate.expire(key, authorizationBO.getExpiresIn(), TimeUnit.SECONDS);
        log.info("expiresIn=====>" + authorizationBO.getExpiresIn());
    }
}
