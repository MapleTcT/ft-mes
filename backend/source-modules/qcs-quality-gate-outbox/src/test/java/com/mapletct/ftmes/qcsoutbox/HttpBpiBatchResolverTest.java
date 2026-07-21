package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class HttpBpiBatchResolverTest {

    private QcsQualityGateOutboxProperties properties;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private HttpBpiBatchResolver resolver;

    @Before
    public void setUp() {
        properties = QcsInternalJwtIssuerTest.properties();
        properties.setBpiBaseUrl("http://bpi-service:19091/");
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        ObjectMapper mapper = new ObjectMapper();
        QcsInternalJwtIssuer issuer = new QcsInternalJwtIssuer(
            properties, mapper, Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));
        resolver = new HttpBpiBatchResolver(properties, issuer, restTemplate);
    }

    @Test
    public void resolvesExactTenantPlantLineAndOrderWithInternalToken() {
        server.expect(once(), requestTo("http://bpi-service:19091/internal/bpi/v1/batches/resolve"
                + "?plantId=PLANT-01&lineId=LINE-S07-01&orderId=ADP_E2E_QCS_OUTBOX_ORDER"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", startsWith("Bearer ")))
            .andRespond(withSuccess("{\"code\":\"OK\",\"data\":{"
                + "\"id\":\"c1584e53-2780-4f58-bb34-9c7399a54d01\","
                + "\"batchNo\":\"BPI-QCS-0001\",\"tenantId\":\"1000\","
                + "\"plantId\":\"PLANT-01\",\"lineId\":\"LINE-S07-01\","
                + "\"orderId\":\"ADP_E2E_QCS_OUTBOX_ORDER\","
                + "\"materialCode\":\"SUGAR-FG-001\",\"state\":\"RELEASED\","
                + "\"revision\":7,\"shadow\":false,\"quantity\":125.5,\"quantityUnit\":\"t\","
                + "\"currentQualityGateId\":\"qcs-inspect:4001\","
                + "\"currentQualityGateRevision\":2,"
                + "\"currentQualityGateSourceEventId\":\"qcs-gate:1000:4001:2\"}}",
                MediaType.APPLICATION_JSON));

        ResolvedBpiBatch batch = resolver.resolve(
            QcsQualityGateProjectorTest.record(1, QcsQualityGateProjectorTest.acceptedInspections()));

        assertEquals("c1584e53-2780-4f58-bb34-9c7399a54d01", batch.getId());
        assertEquals("RELEASED", batch.getState());
        assertEquals("qcs-inspect:4001", batch.getCurrentQualityGateId());
        assertEquals(Long.valueOf(2L), batch.getCurrentQualityGateRevision());
        assertEquals("qcs-gate:1000:4001:2", batch.getCurrentQualityGateSourceEventId());
        server.verify();
    }

    @Test(expected = PermanentQcsOutboxException.class)
    public void rejectsForbiddenScopeAsPermanentConfigurationFailure() {
        server.expect(once(), requestTo("http://bpi-service:19091/internal/bpi/v1/batches/resolve"
                + "?plantId=PLANT-01&lineId=LINE-S07-01&orderId=ADP_E2E_QCS_OUTBOX_ORDER"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        resolver.resolve(QcsQualityGateProjectorTest.record(
            1, QcsQualityGateProjectorTest.acceptedInspections()));
    }

    @Test(expected = RetryableQcsOutboxException.class)
    public void retriesWhenMatchingBpiBatchDoesNotExistYet() {
        server.expect(once(), requestTo("http://bpi-service:19091/internal/bpi/v1/batches/resolve"
                + "?plantId=PLANT-01&lineId=LINE-S07-01&orderId=ADP_E2E_QCS_OUTBOX_ORDER"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        resolver.resolve(QcsQualityGateProjectorTest.record(
            1, QcsQualityGateProjectorTest.acceptedInspections()));
    }

    @Test(expected = PermanentQcsOutboxException.class)
    public void rejectsResolverResponseFromAnotherScope() {
        server.expect(once(), requestTo("http://bpi-service:19091/internal/bpi/v1/batches/resolve"
                + "?plantId=PLANT-01&lineId=LINE-S07-01&orderId=ADP_E2E_QCS_OUTBOX_ORDER"))
            .andRespond(withSuccess("{\"data\":{"
                + "\"id\":\"c1584e53-2780-4f58-bb34-9c7399a54d01\","
                + "\"tenantId\":\"1000\",\"plantId\":\"PLANT-OTHER\","
                + "\"lineId\":\"LINE-S07-01\",\"orderId\":\"ADP_E2E_QCS_OUTBOX_ORDER\","
                + "\"state\":\"CLOSED_RAW\"}}", MediaType.APPLICATION_JSON));

        resolver.resolve(QcsQualityGateProjectorTest.record(
            1, QcsQualityGateProjectorTest.acceptedInspections()));
    }
}
