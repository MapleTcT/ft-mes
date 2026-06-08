package com.supcon.supfusion.flow.taskcenter.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultConsumerConfigFactory {

    @Resource
    private CommonConsumerConfig commonConsumerConfig;

    public <K, V> ConcurrentKafkaListenerContainerFactory<K, V> createContainerFactory(
            Map<String, Object> config, int concurrent) {
        ConcurrentKafkaListenerContainerFactory<K, V> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        factory.setConcurrency(concurrent);

        return factory;
    }

    public Map<String, Object> createDefaultConfig() {
        Map<String, Object> props = new HashMap<>(16);

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, commonConsumerConfig.getServers());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, commonConsumerConfig.getAutoCommit());

        return props;
    }
}
