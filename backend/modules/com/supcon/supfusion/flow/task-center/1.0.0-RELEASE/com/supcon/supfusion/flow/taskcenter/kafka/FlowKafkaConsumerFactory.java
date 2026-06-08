package com.supcon.supfusion.flow.taskcenter.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class FlowKafkaConsumerFactory {

    @Resource
    private DefaultConsumerConfigFactory defaultConsumerConfigFactory;

    @Bean("flowTenantInitContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> flowTenantInitContainerFactory() {
        Map<String, Object> props = this.defaultConsumerConfigFactory.createDefaultConfig();

        String clientId = "flow-tenant-init-consumer";
        String groupId = "flow-tenant-init-event";

        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                this.defaultConsumerConfigFactory.createContainerFactory(props, 1);
        // 多个分区，无需顺序消费
        return factory;
    }

}
