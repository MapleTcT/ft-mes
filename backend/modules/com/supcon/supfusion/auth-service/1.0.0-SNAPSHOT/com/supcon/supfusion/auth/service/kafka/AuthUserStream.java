package com.supcon.supfusion.auth.service.kafka;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * 定义组织架构的通道
 */
public interface AuthUserStream {

    String OUTPUT = "auth-output";
    @Output(OUTPUT)
    MessageChannel authuserOutput();
}
