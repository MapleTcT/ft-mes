package com.supcon.supfusion.rbac.service.kafka.listener;

import com.supcon.supfusion.rbac.service.IUserUrlRefService;
import com.supcon.supfusion.rbac.service.kafka.RbacEventSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author zhang yafei
 */
@Slf4j
//@EnableBinding(RbacEventSink.class)
public class RbacEventStreamListener {

    @Autowired
    private IUserUrlRefService userUrlRefService;

//    @StreamListener(RbacEventSink.INPUT_LOG)
    public void consumeLog(Set<String> apps) {
        log.info("receive event message, payload={}", String.join(",",apps));
//        userUrlRefService.refreshRedis(new ArrayList<>(apps));
    }

}
