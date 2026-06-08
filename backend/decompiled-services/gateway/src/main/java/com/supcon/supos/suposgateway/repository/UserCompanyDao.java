/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Repository
 */
package com.supcon.supos.suposgateway.repository;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserCompanyDao {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Set<Long> getLastCompanyIds(Long userId) {
        String key = String.format("LAST:UID:%d:CIDS", userId);
        Set companyIds = this.stringRedisTemplate.opsForSet().members((Object)key);
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptySet();
        }
        return companyIds.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    public void storeLastCompanyIds(Long userId, Set<Long> companyIds) {
        String key = String.format("LAST:UID:%d:CIDS", userId);
        this.stringRedisTemplate.delete((Object)key);
        if (companyIds == null || companyIds.isEmpty()) {
            return;
        }
        Object[] companyIdArray = (String[])companyIds.stream().map(Object::toString).toArray(String[]::new);
        this.stringRedisTemplate.opsForSet().add((Object)key, companyIdArray);
    }
}

