/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.redisson.client.codec.Codec
 *  org.redisson.config.ClusterServersConfig
 *  org.redisson.config.Config
 *  org.redisson.config.SentinelServersConfig
 *  org.redisson.config.SingleServerConfig
 *  org.redisson.spring.starter.RedissonAutoConfiguration
 *  org.redisson.spring.starter.RedissonAutoConfigurationCustomizer
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.AutoConfigureAfter
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Primary
 *  org.springframework.data.redis.connection.RedisConnectionFactory
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.serializer.RedisSerializer
 */
package com.supcon.supos.suposgateway.cache.redisson;

import com.supcon.supos.suposgateway.cache.redisson.GenericFastJsonCodec;
import com.supcon.supos.suposgateway.cache.redisson.RedissonProperties;
import org.redisson.client.codec.Codec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@AutoConfigureAfter(value={org.redisson.spring.starter.RedissonAutoConfiguration.class})
@EnableConfigurationProperties(value={RedissonProperties.class})
public class RedissonAutoConfiguration
implements RedissonAutoConfigurationCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(org.redisson.spring.starter.RedissonAutoConfiguration.class);
    @Autowired
    private RedissonProperties redissonProperties;

    @Primary
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate template = new RedisTemplate();
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    public void customize(Config config) {
        try {
            config.setThreads(this.redissonProperties.getThreads());
            config.setNettyThreads(this.redissonProperties.getNettyThreads());
            config.setCodec((Codec)GenericFastJsonCodec.INSTANCE);
            if (config.isSentinelConfig()) {
                SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
                sentinelServersConfig.setMasterConnectionPoolSize(this.redissonProperties.getPoolSize());
                sentinelServersConfig.setSlaveConnectionPoolSize(this.redissonProperties.getPoolSize());
                sentinelServersConfig.setSubscriptionConnectionPoolSize(this.redissonProperties.getPoolSize());
                sentinelServersConfig.setMasterConnectionMinimumIdleSize(this.redissonProperties.getMinIdleSize());
                sentinelServersConfig.setSlaveConnectionMinimumIdleSize(this.redissonProperties.getMinIdleSize());
                sentinelServersConfig.setIdleConnectionTimeout(this.redissonProperties.getIdleConnTimeout());
                sentinelServersConfig.setFailedSlaveCheckInterval(this.redissonProperties.getFailedSlaveCheckInterval());
                sentinelServersConfig.setKeepAlive(this.redissonProperties.isKeepAlive());
                sentinelServersConfig.setPingConnectionInterval(this.redissonProperties.getPingConnectionInterval());
            } else if (config.isClusterConfig()) {
                ClusterServersConfig clusterServersConfig = config.useClusterServers();
                clusterServersConfig.setMasterConnectionPoolSize(this.redissonProperties.getPoolSize());
                clusterServersConfig.setSlaveConnectionPoolSize(this.redissonProperties.getPoolSize());
                clusterServersConfig.setSubscriptionConnectionPoolSize(this.redissonProperties.getPoolSize());
                clusterServersConfig.setMasterConnectionMinimumIdleSize(this.redissonProperties.getMinIdleSize());
                clusterServersConfig.setSlaveConnectionMinimumIdleSize(this.redissonProperties.getMinIdleSize());
                clusterServersConfig.setIdleConnectionTimeout(this.redissonProperties.getIdleConnTimeout());
                clusterServersConfig.setFailedSlaveCheckInterval(this.redissonProperties.getFailedSlaveCheckInterval());
                clusterServersConfig.setKeepAlive(this.redissonProperties.isKeepAlive());
                clusterServersConfig.setPingConnectionInterval(this.redissonProperties.getPingConnectionInterval());
            } else {
                SingleServerConfig singleServerConfig = config.useSingleServer();
                singleServerConfig.setConnectionPoolSize(this.redissonProperties.getPoolSize());
                singleServerConfig.setSubscriptionConnectionPoolSize(this.redissonProperties.getPoolSize());
                singleServerConfig.setConnectionMinimumIdleSize(this.redissonProperties.getMinIdleSize());
                singleServerConfig.setIdleConnectionTimeout(this.redissonProperties.getIdleConnTimeout());
                singleServerConfig.setKeepAlive(this.redissonProperties.isKeepAlive());
                singleServerConfig.setPingConnectionInterval(this.redissonProperties.getPingConnectionInterval());
            }
        }
        catch (Exception e) {
            LOGGER.error("redisson customizer error.", (Throwable)e);
        }
    }
}

