package com.supcon.supfusion.auth.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.TimeUnit;

@Slf4j
public class SpringEventListListener implements ApplicationListener<SpringApplicationEvent> {


    public volatile static Boolean isSuccess = false;

    @Override
    public void onApplicationEvent(SpringApplicationEvent ev) {
        if (ev instanceof ApplicationEnvironmentPreparedEvent) {
            log.info("ApplicationEnvironmentPreparedEvent");
            ThreadPoolUtils.onlineUserService.schedule(() -> {
                log.error("isSuccess is error");
                if (!isSuccess) {
                    System.exit(-1);
                }
            }, 10, TimeUnit.MINUTES);
        } else if (ev instanceof ApplicationFailedEvent) {
            log.error("spring application run failed");
//            System.exit(-1);
        }
    }

}
