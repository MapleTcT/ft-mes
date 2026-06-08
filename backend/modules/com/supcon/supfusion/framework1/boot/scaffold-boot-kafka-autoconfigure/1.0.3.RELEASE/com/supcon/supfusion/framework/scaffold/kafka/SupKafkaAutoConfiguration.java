package com.supcon.supfusion.framework.scaffold.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.framework.scaffold.kafka.converter.SupMappingJackson2MessageConverter;
import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventStreamListener;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.stream.annotation.StreamMessageConverter;
import org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * @author zhouchengzhang
 * @date 2020/4/14
 */
@Setter
@Getter
@Configuration
@AutoConfigureAfter({KafkaAutoConfiguration.class, KafkaBinderConfiguration.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class SupKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @StreamMessageConverter
    public SupMappingJackson2MessageConverter supMappingJackson2MessageConverter(ObjectMapper objectMapper) {
        return new SupMappingJackson2MessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantEventStreamListener tenantEventStreamListener() {
        return new TenantEventStreamListener();
    }
}
