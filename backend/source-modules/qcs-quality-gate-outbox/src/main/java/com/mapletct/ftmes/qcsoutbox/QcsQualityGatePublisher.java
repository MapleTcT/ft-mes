package com.mapletct.ftmes.qcsoutbox;

import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;

public interface QcsQualityGatePublisher {
    void publish(String topic, String key, QcsQualityGateV1 event) throws Exception;
}
