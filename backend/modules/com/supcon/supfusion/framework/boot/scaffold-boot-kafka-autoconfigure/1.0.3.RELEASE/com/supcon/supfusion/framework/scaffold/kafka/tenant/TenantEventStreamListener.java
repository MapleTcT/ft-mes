package com.supcon.supfusion.framework.scaffold.kafka.tenant;

import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventPublisher;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author tomcat
 * @date 20-6-4 下午8:39
 */
@Setter
@Getter
@Slf4j
public class TenantEventStreamListener {

    @Autowired
    private TenantEventPublisher tenantEventPublisher;

    @StreamListener(TenantEventSink.INPUT)
    public void handle(@Payload Message<TenantInfo> message) {
        log.info("receive tenant event message, payload={}", message);
        // 处理
        TenantInfo tenantInfo = message.getBody();
        switch (tenantInfo.getEventType()) {
            case ADD:{
                tenantEventPublisher.publishAdd(new TenantAddEvent(this, tenantInfo));
                break;
            }
            case DESTROY: {
                tenantEventPublisher.publishDestory(new TenantDestroyEvent(this, tenantInfo));
                break;
            }
            default:
        }
    }
}
