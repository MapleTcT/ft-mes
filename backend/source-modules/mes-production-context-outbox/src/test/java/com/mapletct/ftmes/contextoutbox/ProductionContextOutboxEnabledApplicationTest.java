package com.mapletct.ftmes.contextoutbox;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:context_outbox_enabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mes.production-context.outbox.enabled=true",
        "mes.production-context.outbox.poll-delay-ms=60000",
        "mes.production-context.kafka.bootstrap-servers=127.0.0.1:1"
    }
)
public class ProductionContextOutboxEnabledApplicationTest {

    @MockBean
    private ProductionContextOutboxRepository repository;

    @MockBean
    private ProductionContextPublisher publisher;

    @Autowired
    private ProductionContextOutboxDispatcher dispatcher;

    @Test
    public void contextLoadsWithPublisherEnabled() {
        assertNotNull(dispatcher);
    }
}
