package com.supcon.supfusion.rbac.service.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author zhang yafei
 */
public interface RbacEventSink {
    String INPUT_LOG = "rbac_input_log";
    String OUTPUT_LOG = "rbac_output_log";

    @Input(INPUT_LOG)
    SubscribableChannel consumeLog();


    /**
     * 缺省发送消息通道
     * @return channel 返回缺省信息发送通道
     */
    @Output(OUTPUT_LOG)
    MessageChannel produceLog();
}
