package com.supcon.supfusion.signature.services.kafka.listener;

import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.services.kafka.SignatureEventSink;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author zhang yafei
 */
@Slf4j
@EnableBinding(SignatureEventSink.class)
public class SignatrueEventStreamListener {

    @Autowired
    SignatureLogService signatureLogService;

    @StreamListener(SignatureEventSink.SIGNATURE_INPUT_LOG)
    public void saveSignatureLog(List<SignatureLog> signatureLogs) {
        log.info("receive tenant event message, payload={}", signatureLogs);
        signatureLogs.forEach(signatureLog -> {
            signatureLog.setUuid(UUID.randomUUID().toString().replace("-",""));
            signatureLog.setCreateTime(new Date());
        });
        signatureLogService.batchSaveSignatureLog(signatureLogs);
    }

}
