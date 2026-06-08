/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  com.github.benmanes.caffeine.cache.LoadingCache
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnClass
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.annotation.CachingConfigurerSupport
 *  org.springframework.cache.annotation.EnableCaching
 *  org.springframework.cache.caffeine.CaffeineCache
 *  org.springframework.cache.caffeine.CaffeineCacheManager
 *  org.springframework.cache.interceptor.CacheOperationInvocationContext
 *  org.springframework.cache.interceptor.CacheResolver
 *  org.springframework.cache.interceptor.KeyGenerator
 *  org.springframework.cache.interceptor.SimpleCacheResolver
 *  org.springframework.cache.support.SimpleCacheManager
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Primary
 *  org.springframework.util.StringUtils
 */
package com.supcon.supos.suposgateway.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass(value={Caffeine.class, CaffeineCacheManager.class})
@EnableCaching
public class CaffeineAutoConfiguration
extends CachingConfigurerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineAutoConfiguration.class);

    @Primary
    @Bean
    public CacheManager cacheManager() {
        LoadingCache defaultCache = Caffeine.newBuilder().maximumSize(1024L).expireAfterWrite(30L, TimeUnit.SECONDS).build(this::loadKey);
        LoadingCache secretKeyCache = Caffeine.newBuilder().maximumSize(1024L).expireAfterWrite(300L, TimeUnit.SECONDS).build(this::loadKey);
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(new CaffeineCache("defaultCache", (Cache)defaultCache), new CaffeineCache("secretKeyCache", (Cache)secretKeyCache)));
        return cacheManager;
    }

    private Object loadKey(Object key) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Caffeine load cache key: " + key);
        }
        return null;
    }

    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(":");
            sb.append(method.getName());
            sb.append(":");
            sb.append(StringUtils.arrayToDelimitedString((Object[])params, (String)":"));
            return sb.toString();
        };
    }

    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(Objects.requireNonNull(this.cacheManager())){

            protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
                List<String> result = super.getCacheNames(context);
                if (result == null || result.isEmpty()) {
                    result = Collections.singletonList("defaultCache");
                }
                return result;
            }
        };
    }
}

