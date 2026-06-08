package com.supcon.supfusion.auth.service.cache;

import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.service.AuthLoginLogService;
import com.supcon.supfusion.auth.service.OnlineUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 监听redis key过期事件
 *
 * @author caokele
 */
@Slf4j
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    private static final String AUTH_TICKET = "AUTH:TICKET";
    @Autowired
    private OnlineUserService onlineUserService;
    @Autowired
    private AuthLoginLogService authLoginLogService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        // 删除在线用户
        if (key.startsWith(AUTH_TICKET)) {
            log.info("Expire key: {}", key);
            String ticket = key.substring(AUTH_TICKET.length() + 1);
            onlineUserService.removeSessionByTicketWithKeyCloak(ticket, null);
            //保存超时登出日志
            authLoginLogService.saveLogoutLog(ticket, Constants.TIME_OUT_LOGOUT);
        }
    }


    @SuppressWarnings("ping 会阻塞，可能导致其他任务卡住")
    @Scheduled(cron = "0/10 * * * * *")
    public void init1() {

        long start = System.currentTimeMillis();
        //will blockd if not return currently
        String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
        long now = System.currentTimeMillis();
        if ((now - start) > 1000 * 1) {
            super.init();
        }
    }

}
