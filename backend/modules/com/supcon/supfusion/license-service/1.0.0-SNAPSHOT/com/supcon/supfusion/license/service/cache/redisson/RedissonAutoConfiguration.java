package com.supcon.supfusion.license.service.cache.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.Resource;

@Configuration("licenseRedissonAutoConfiguration")
@AutoConfigureAfter(org.redisson.spring.starter.RedissonAutoConfiguration.class)
@EnableConfigurationProperties(RedissonProperties.class)
@Slf4j
public class RedissonAutoConfiguration implements RedissonAutoConfigurationCustomizer {
    @Resource
    private RedissonProperties redissonProperties;

    @Primary
    @Bean("licenseRedisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setKeySerializer(RedisSerializer.string());
//        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
//        template.setHashValueSerializer(RedisSerializer.json());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean("licenseStringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        // FIXME: 这边如果使用事务，会导致在事务中无法获取到值
          // template.setEnableTransactionSupport(true);
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Override
    public void customize(Config config) {
        try {
            config.setThreads(redissonProperties.getThreads());
            config.setNettyThreads(redissonProperties.getNettyThreads());
            config.setCodec(GenericFastJsonCodec.INSTANCE);
            if (config.isSentinelConfig()) {
                SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
                sentinelServersConfig.setMasterConnectionPoolSize(redissonProperties.getPoolSize());
                sentinelServersConfig.setSlaveConnectionPoolSize(redissonProperties.getPoolSize());
                sentinelServersConfig.setSubscriptionConnectionPoolSize(redissonProperties.getPoolSize());
                sentinelServersConfig.setMasterConnectionMinimumIdleSize(redissonProperties.getMinIdleSize());
                sentinelServersConfig.setSlaveConnectionMinimumIdleSize(redissonProperties.getMinIdleSize());
                sentinelServersConfig.setIdleConnectionTimeout(redissonProperties.getIdleConnTimeout());
                sentinelServersConfig.setFailedSlaveCheckInterval(redissonProperties.getFailedSlaveCheckInterval());
            } else if (config.isClusterConfig()) {
                ClusterServersConfig clusterServersConfig = config.useClusterServers();
                clusterServersConfig.setMasterConnectionPoolSize(redissonProperties.getPoolSize());
                clusterServersConfig.setSlaveConnectionPoolSize(redissonProperties.getPoolSize());
                clusterServersConfig.setSubscriptionConnectionPoolSize(redissonProperties.getPoolSize());
                clusterServersConfig.setMasterConnectionMinimumIdleSize(redissonProperties.getMinIdleSize());
                clusterServersConfig.setSlaveConnectionMinimumIdleSize(redissonProperties.getMinIdleSize());
                clusterServersConfig.setIdleConnectionTimeout(redissonProperties.getIdleConnTimeout());
                clusterServersConfig.setFailedSlaveCheckInterval(redissonProperties.getFailedSlaveCheckInterval());
            } else {
                SingleServerConfig singleServerConfig = config.useSingleServer();
                singleServerConfig.setConnectionPoolSize(redissonProperties.getPoolSize());
                singleServerConfig.setSubscriptionConnectionPoolSize(redissonProperties.getPoolSize());
                singleServerConfig.setConnectionMinimumIdleSize(redissonProperties.getMinIdleSize());
                singleServerConfig.setIdleConnectionTimeout(redissonProperties.getIdleConnTimeout());
            }
        } catch (Exception e) {
            log.error("redisson customizer error.", e);
        }
    }

    @Bean("licenseRedisMessageListenerContainer")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

}
