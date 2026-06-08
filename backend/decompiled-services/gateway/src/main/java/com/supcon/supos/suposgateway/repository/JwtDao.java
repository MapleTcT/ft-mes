/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.redisson.api.RMap
 *  org.redisson.api.RedissonClient
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cache.annotation.Cacheable
 *  org.springframework.stereotype.Repository
 */
package com.supcon.supos.suposgateway.repository;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JwtDao {
    @Autowired
    private RedissonClient redissonClient;

    @Cacheable
    public String get(String token) {
        RMap userSession = this.redissonClient.getMap(token);
        return (String)userSession.get((Object)"jwt");
    }
}

