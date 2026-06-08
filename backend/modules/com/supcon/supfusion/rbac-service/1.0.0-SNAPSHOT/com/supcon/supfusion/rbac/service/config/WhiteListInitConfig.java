package com.supcon.supfusion.rbac.service.config;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.List;

@Configuration
@PropertySource(value = "classpath:whiteList.yml", encoding = "utf-8", factory = ConfigParamFactory.class)
public class WhiteListInitConfig implements CommandLineRunner {
    @Value("${urls}")
    private List<String> urls;
    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        //启动加载白名单数据到redis
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set("whiteUrl",urls);
    }
}
