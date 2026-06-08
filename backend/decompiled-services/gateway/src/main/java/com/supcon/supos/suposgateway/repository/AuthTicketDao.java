/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Repository
 */
package com.supcon.supos.suposgateway.repository;

import com.supcon.supos.suposgateway.feign.dto.AuthorizationDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthTicketDao {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void storeAuthorization(String ticket, AuthorizationDTO authorizationDTO) {
        HashMap<String, String> authorizationMap = new HashMap<String, String>();
        authorizationMap.put("access_token", authorizationDTO.getAccessToken());
        authorizationMap.put("refresh_token", authorizationDTO.getRefreshToken());
        authorizationMap.put("token_type", authorizationDTO.getTokenType());
        if (authorizationDTO.getCompanyId() != null) {
            authorizationMap.put("companyId", authorizationDTO.getCompanyId().toString());
        }
        String key = String.format("AUTH:TICKET:%s", ticket);
        this.stringRedisTemplate.opsForHash().putAll((Object)key, authorizationMap);
        this.stringRedisTemplate.expire((Object)key, (long)authorizationDTO.getExpiresIn().intValue(), TimeUnit.SECONDS);
    }

    public Map<Object, Object> getMapByTicket(String ticket) {
        String key = String.format("AUTH:TICKET:%s", ticket);
        return this.stringRedisTemplate.opsForHash().entries((Object)key);
    }

    public void deleteByTicket(String ticket) {
        String key = String.format("AUTH:TICKET:%s", ticket);
        this.stringRedisTemplate.delete((Object)key);
    }
}

