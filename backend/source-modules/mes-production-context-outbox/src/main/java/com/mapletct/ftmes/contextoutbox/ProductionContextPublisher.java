package com.mapletct.ftmes.contextoutbox;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;

public interface ProductionContextPublisher {

    void publish(String topic, String key, ProductionContextEventV1 event) throws Exception;
}
