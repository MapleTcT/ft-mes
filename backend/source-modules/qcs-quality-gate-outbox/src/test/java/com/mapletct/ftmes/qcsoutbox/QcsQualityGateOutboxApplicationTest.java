package com.mapletct.ftmes.qcsoutbox;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QcsQualityGateOutboxApplication.class)
@TestPropertySource(properties = {
    "qcs.bpi.outbox.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:qcs_outbox_disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.task.scheduling.enabled=false"
})
public class QcsQualityGateOutboxApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void disabledByDefaultDoesNotCreateDispatcherOrKafkaPublisher() {
        assertFalse(context.containsBeanDefinition("qcsQualityGateOutboxDispatcher"));
        assertFalse(context.containsBeanDefinition("kafkaQcsQualityGatePublisher"));
    }
}
