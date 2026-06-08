package com.supcon.supfusion.auth.service.config;

import com.supcon.supfusion.auth.common.utils.Base64Util;
import com.supcon.supfusion.auth.dao.mapper.OnlineUserMapper;
import com.supcon.supfusion.auth.dao.po.OnlineUserPO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "integration.supos.enabled", havingValue = "false")
public class LoginNumConfig {

    @Autowired
    private AuthTicketCache authTicketCache;

    @Autowired
    private OnlineUserMapper onlineUserMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String licenseRedisKey = "LICENSE:INFO";

    private static final String SPLIT = "/";

    @Bean
    public void cacheRequestBodyGlobalFilter() {
        List<OnlineUserPO> onlineUserPOList = onlineUserMapper.selectAll();
        // 删除redis ticket缓存
        for (OnlineUserPO onlineUserPO : onlineUserPOList) {
            authTicketCache.deleteByTicket(onlineUserPO.getTicket());
        }

        //项目启动删除在线用户数据
        onlineUserMapper.deleteAll();
    }

    /**
     * 定时任务刷新登录数
     */
    @Scheduled(cron = "*/10 * * * * ?")
    public void refreshLoginNum() {
        //获取授权登录数
        Integer valueLicenseRedis = 5;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(licenseRedisKey);
        if (!ObjectUtils.isEmpty(entries)) {
            for (Map.Entry<Object, Object> redisEntry : entries.entrySet()) {
                String[] keySplit = ((String) redisEntry.getKey()).split(SPLIT);
                String moduleCodeRedis = Base64Util.decode(keySplit[0]);
                if (moduleCodeRedis.equals("supPlant-Server-S0C")) {
                    String[] split = ((String) redisEntry.getValue()).split(SPLIT);
                    valueLicenseRedis = Integer.valueOf(Base64Util.decode(split[0]));
                    if (valueLicenseRedis.equals(-1)) {
                        valueLicenseRedis = 5;
                    }
                }
            }
        }

        //获取在线用户数
        Integer onlineNum = onlineUserMapper.selectTotalCount();

        //将在线人数和授权人数存入redis
        redisTemplate.opsForValue().set("LICENSE:LOGIN_NUM", valueLicenseRedis + "," + onlineNum);
    }

}

