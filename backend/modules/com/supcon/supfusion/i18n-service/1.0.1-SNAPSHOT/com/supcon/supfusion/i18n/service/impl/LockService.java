package com.supcon.supfusion.i18n.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.i18n.common.until.Constants;

@Service
public class LockService {

	@Qualifier("i18nRedisTemplate")
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	/**
	 * 获取锁, 如果没有手动释放, 那么30分钟后释放锁
	 * @param lockName
	 * @return
	 */
	public boolean acquire(String lockName) {
		return redisTemplate.opsForValue().setIfAbsent(lockName, Constants.ONE_STR, 30, TimeUnit.MINUTES);
	}
	
	/**
	 * 判断是否有锁存在
	 * @param lockName
	 * @return
	 */
	public boolean isLocked(String lockName) {
		Object lock = redisTemplate.opsForValue().get(lockName);
		return lock != null;
	}
	
	/**
	 * 释放锁
	 * @param lockName
	 */
	public void release(String lockName) {
		redisTemplate.delete(lockName);
	}
}


