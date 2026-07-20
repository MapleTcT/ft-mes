package com.mapletct.ftmes.bpiwmsadapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MaterialWmsHttpClientTest {

    private MaterialWmsHttpClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        BpiWmsAdapterProperties properties = properties();
        client = new MaterialWmsHttpClient(
                properties,
                new ObjectMapper(),
                new RestTemplateBuilder()
                        .rootUri(properties.materialBaseUrl())
                        .build());
        server = MockRestServiceServer.bindTo(client.restTemplate()).build();
    }

    @Test
    void exactLookupUsesTenantAndIntegrationKeyAndParsesDurableDocument() {
        String key = "WMS_COMPLETION_INBOUND|TENANT|BATCH|GATE|1";
        server.expect(requestTo("http://material:8080/material/wms/completion-inbounds/by-idempotency"
                        + "?sourceSystem=BPI&idempotencyKey=WMS_COMPLETION_INBOUND%7CTENANT%7CBATCH%7CGATE%7C1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tenant-Id", "TENANT"))
                .andExpect(header("X-BPI-WMS-Key", "test-key"))
                .andRespond(withSuccess(foundBody(key), MediaType.APPLICATION_JSON));

        MaterialWmsDocument document = client.findByIdempotency("TENANT", key).orElseThrow();

        assertThat(document.documentNo()).isEqualTo("CI-001");
        assertThat(document.lines()).singleElement()
                .extracting(MaterialWmsDocument.Line::unitCode)
                .isEqualTo("kg");
        server.verify();
    }

    @Test
    void exactLookupTreatsLegacyEnvelope404AsNotFound() {
        String key = "MISSING";
        server.expect(requestTo("http://material:8080/material/wms/completion-inbounds/by-idempotency"
                        + "?sourceSystem=BPI&idempotencyKey=MISSING"))
                .andRespond(withSuccess(
                        "{\"code\":404,\"data\":null,\"message\":\"not found\"}",
                        MediaType.APPLICATION_JSON));

        Optional<MaterialWmsDocument> result = client.findByIdempotency("TENANT", key);

        assertThat(result).isEmpty();
        server.verify();
    }

    private BpiWmsAdapterProperties properties() {
        return new BpiWmsAdapterProperties(
                true, "kafka:9092", "command", "command-dlq", "receipt",
                "group", "client", "http://material:8080", "test-key", "Asia/Shanghai",
                65_536, 5, 1, Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1), List.of("TENANT|PLANT|LINE|WARE|LOC|COMP|kg"));
    }

    private String foundBody(String key) {
        return "{\"code\":200,\"message\":\"success\",\"data\":{"
                + "\"document\":{\"id\":101,\"document_no\":\"CI-001\","
                + "\"source_system\":\"BPI\",\"source_document_id\":\"EVENT-1\","
                + "\"idempotency_key\":\"" + key + "\",\"warehouse_code\":\"WARE\","
                + "\"status\":\"POSTED\",\"quality_status\":\"QUALIFIED\"},"
                + "\"lines\":[{\"source_system\":\"BPI\",\"source_line_id\":\"EVENT-1:1\","
                + "\"material_code\":\"MAT\",\"batch_no\":\"BATCH\","
                + "\"production_batch_no\":\"BATCH\",\"warehouse_code\":\"WARE\","
                + "\"location_code\":\"LOC\",\"quantity\":10,\"unit_code\":\"kg\","
                + "\"quality_status\":\"QUALIFIED\"}],\"transactions\":[]}}";
    }
}
