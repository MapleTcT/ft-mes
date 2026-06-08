package com.supcon.supfusion.framework.scaffold.redis;

import com.supcon.supfusion.framework.scaffold.redis.properties.SupfusionRedisProperties;
import com.supcon.supfusion.framework.scaffold.redis.serializer.TenantStringRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Slf4j
@Configuration
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties({RedisProperties.class, SupfusionRedisProperties.class})
@Import({JedisConnectionConfiguration.class})
public class RedisAutoConfiguration {

    /**
     * 默认30min
     */
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30L);

    private SupfusionRedisProperties supfusionRedisProperties;

    public RedisAutoConfiguration(SupfusionRedisProperties supfusionRedisProperties) {
        this.supfusionRedisProperties = supfusionRedisProperties;
    }

    private RedisSerializer tenantStringRedisSerializer() {
        if (Objects.equals(Boolean.TRUE, supfusionRedisProperties.getUseTenantPrefix())) {
            return new TenantStringRedisSerializer();
        } else {
            return RedisSerializer.string();
        }
    }

    private GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        //  替换开发团队定义的规范的json序列化
        return new GenericJackson2JsonRedisSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(tenantStringRedisSerializer());
        template.setValueSerializer(genericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(tenantStringRedisSerializer());
        template.setHashValueSerializer(genericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(tenantStringRedisSerializer());
        template.setValueSerializer(genericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(tenantStringRedisSerializer());
        template.setHashValueSerializer(genericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * @param ttlStr
     * @return
     */
    private Duration getDuration(String ttlStr) {
        Duration duration = null;

        if (StringUtils.isEmpty(ttlStr)) {
            duration = DEFAULT_DURATION;
        } else {
            try {
                duration = Duration.parse(ttlStr);
            } catch (DateTimeParseException e) {
                //
                log.warn("CacheManager Duration's param[spring.redis.cache-ttl] parse error:{}!", new Object[]{e.getMessage()});
                duration = DEFAULT_DURATION;
            }
        }

        return duration;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 缓存配置对象
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();

        Duration duration = getDuration(this.supfusionRedisProperties.getCacheTtl());

        //设置缓存的默认超时时间：30分钟
        redisCacheConfiguration = redisCacheConfiguration.entryTtl(duration);
        if (Objects.equals(Boolean.TRUE, this.supfusionRedisProperties.getCacheNullValues())) {
            //如果是空值，不缓存
            redisCacheConfiguration = redisCacheConfiguration.disableCachingNullValues();
        }

        redisCacheConfiguration = redisCacheConfiguration
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(tenantStringRedisSerializer()))                //设置key序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer((genericJackson2JsonRedisSerializer())));    //设置value序列化器

        return RedisCacheManager
                .builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(redisCacheConfiguration).build();
    }

}
