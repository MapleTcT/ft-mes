package com.supcon.supfusion.organization.common.kafka;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * 定义组织架构的通道
 */
public interface OrganizationStream {

    String OUTPUT = "organization-output";
    @Output(OUTPUT)
    MessageChannel organizationOutput();
}
