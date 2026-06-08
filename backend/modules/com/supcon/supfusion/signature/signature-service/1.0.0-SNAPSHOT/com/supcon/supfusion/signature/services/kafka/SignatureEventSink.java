package com.supcon.supfusion.signature.services.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author zhang yafei
 */
public interface SignatureEventSink {
    String SIGNATURE_INPUT_LOG = "signature_input_log";
    String SIGNATURE_OUTPUT_LOG = "signature-output_log";

    @Input(SIGNATURE_INPUT_LOG)
    SubscribableChannel saveSignatureLog();


    /**
     * 缺省发送消息通道
     * @return channel 返回缺省信息发送通道
     */
    @Output(SIGNATURE_OUTPUT_LOG)
    MessageChannel sendSignatureLog();
}
