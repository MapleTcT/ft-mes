package com.mapletct.ftmes.bpiwmsadapter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.wms-adapter", name = "enabled", havingValue = "true")
public class WmsReversalCommandKafkaListener {

    private final WmsReversalCommandProcessor processor;

    public WmsReversalCommandKafkaListener(WmsReversalCommandProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${bpi.wms-adapter.reversal-command-topic}",
            containerFactory = "bpiWmsAdapterListenerContainerFactory")
    public void onCommand(
            ConsumerRecord<byte[], byte[]> record,
            Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
