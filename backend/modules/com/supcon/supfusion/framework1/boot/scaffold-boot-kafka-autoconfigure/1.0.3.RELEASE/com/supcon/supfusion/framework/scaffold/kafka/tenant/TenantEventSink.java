package com.supcon.supfusion.framework.scaffold.kafka.tenant;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author tomcat
 * @date 20-6-4 上午10:24
 */
public interface TenantEventSink {

    String INPUT = "tenant-input";

    @Input(INPUT)
    SubscribableChannel inboundChannel();
}
