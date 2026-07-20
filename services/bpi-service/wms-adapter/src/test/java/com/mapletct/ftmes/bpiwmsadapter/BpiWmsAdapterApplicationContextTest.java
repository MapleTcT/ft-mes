package com.mapletct.ftmes.bpiwmsadapter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "bpi.wms-adapter.enabled=false")
class BpiWmsAdapterApplicationContextTest {

    @Test
    void disabledByDefaultContextStartsWithoutKafkaOrMaterialWrites() {
    }
}
