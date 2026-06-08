package com.supcon.supfusion.flow.taskcenter.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonConsumerConfig {
    @Value("${spring.cloud.stream.kafka.binder.brokers}")
    private String servers;

    private Boolean autoCommit = true;

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }
}
