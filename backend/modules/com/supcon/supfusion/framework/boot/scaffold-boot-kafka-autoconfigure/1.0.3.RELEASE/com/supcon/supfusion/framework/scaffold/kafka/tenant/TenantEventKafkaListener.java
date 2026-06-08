package com.supcon.supfusion.framework.scaffold.kafka.tenant;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventPublisher;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author tomcat
 * @date 20-6-13 下午4:40
 */
@Slf4j
@Setter
@Getter
public class TenantEventKafkaListener {

    @Autowired
    private ObjectMapper         objectMapper;
    @Autowired
    private TenantEventPublisher tenantEventPublisher;

    @KafkaListener(topics = "supOS_tenant", groupId = "${spring.application.name}")
    public void handle(ConsumerRecord<String, Object> record) {
        Message<TenantInfo> message = null;
        if (record.value() instanceof String) {
            String json = (String) record.value();
            log.info("receive tenant event message, msg={}", json);
            try {
                message = objectMapper.readValue(json, new TypeReference<Message<TenantInfo>>() {});
            } catch (IOException e) {
                Log.error("parse json failed when receive tenant event message", e);
            }
        } else {
            try {
                String json = new String((byte[]) record.value(), StandardCharsets.UTF_8);
                log.info("receive tenant event message, msg={}", json);
                message = objectMapper.readValue(json, new TypeReference<Message<TenantInfo>>() {});
            } catch (Exception e) {
                Log.error("parse json failed when receive tenant event message", e);
            }
        }
        if (message != null) {
            // 处理
            TenantInfo tenantInfo = message.getBody();
            switch (tenantInfo.getEventType()) {
                case ADD: {
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
}
