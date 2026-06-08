/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.component;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author: zhuangmh
 * @date: 2020年8月27日 上午8:52:35
 */
@Component
public class RedisUtils {

    private static final int DEFAULT_TIME_OUT = 12 * 3600 * 1000; // 12小时
    private RedisUtils() {

    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取单个字符串值
     * @param key
     * @return
     */
    public String getStringValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ignore) {
            
        }
        return null;
    }
    
    /**
     * 设置单个key-value, 默认超时时间为12小时
     * @param key
     * @param value
     */
    public void setStringValue(String key, String value) {
        setStringValue(key, value, DEFAULT_TIME_OUT);
    }
    
    /**
     * 设置单个key-value, 自定义超时时间
     * @param key
     * @param value
     * @param timeout
     */
    public void setStringValue(String key, String value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
            
        }
    }
}
