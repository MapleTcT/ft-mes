package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiTelemetryPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
        registry.add("bpi.telemetry.http-ingress-enabled", () -> "true");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    private String tenantId;
    private String marker;
    private String ingestToken;
    private long sampleTime;

    @BeforeEach
    void prepareMarker() throws Exception {
        marker = "ADP_E2E_TELEMETRY_" + UUID.randomUUID().toString().replace("-", "");
        tenantId = marker;
        ingestToken = token(tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        sampleTime = Instant.now().minusSeconds(5).toEpochMilli();
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_telemetry_quarantine WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_point_rejects WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_points WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_source_state WHERE tenant_id = ?", tenantId);
    }

    @Test
    void telemetryIngressPersistsValidFactsAndEnforcesReplaySequenceAndScopeSemantics() throws Exception {
        ObjectNode first = envelope("EVT-1", 1, 1);
        addDoublePoint(first, "flow.instant", 18.6, "t/h", "GOOD");
        addBooleanPoint(first, "pump.running", true, "bool", "GOOD");
        addDoublePoint(first, "raw.baume", 21.2, "unsupported-unit", "GOOD");

        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .header("X-Trace-Id", marker + "-TRACE-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventId").value(marker + "-EVT-1"))
                .andExpect(jsonPath("$.data.status").value("PARTIAL"))
                .andExpect(jsonPath("$.data.sequenceDisposition").value("FIRST"))
                .andExpect(jsonPath("$.data.acceptedPoints").value(2))
                .andExpect(jsonPath("$.data.rejectedPoints").value(1))
                .andExpect(jsonPath("$.data.replay").value(false));

        assertThat(count("bpi_telemetry_events")).isEqualTo(1);
        assertThat(count("bpi_telemetry_points")).isEqualTo(2);
        assertThat(count("bpi_telemetry_point_rejects")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT status FROM bpi.bpi_telemetry_events
                 WHERE tenant_id = ? AND event_id = ?
                """, String.class, tenantId, marker + "-EVT-1")).isEqualTo("PARTIAL");
        assertThat(jdbc.queryForList("""
                SELECT property_id FROM bpi.bpi_telemetry_points
                 WHERE tenant_id = ? ORDER BY property_id
                """, String.class, tenantId)).containsExactly("flow.instant", "pump.running");

        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replay").value(true));
        assertThat(count("bpi_telemetry_events")).isEqualTo(1);
        assertThat(count("bpi_telemetry_points")).isEqualTo(2);

        ObjectNode reusedEvent = first.deepCopy();
        reusedEvent.put("messageId", marker + "-CHANGED");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(reusedEvent)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("eventId")));

        ObjectNode reusedSourceIdentity = envelope("EVT-OTHER", 1, 1);
        addDoublePoint(reusedSourceIdentity, "flow.instant", 19.1, "t/h", "GOOD");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(reusedSourceIdentity)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("epoch/sequence")));

        ObjectNode gap = envelope("EVT-3", 1, 3);
        addDoublePoint(gap, "flow.instant", 20.4, "t/h", "GOOD");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(gap)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sequenceDisposition").value("GAP"));
        assertThat(sourceSequence()).isEqualTo(3L);

        ObjectNode outOfOrder = envelope("EVT-2", 1, 2);
        addDoublePoint(outOfOrder, "flow.instant", 19.8, "t/h", "GOOD");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(outOfOrder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sequenceDisposition").value("OUT_OF_ORDER"));
        assertThat(sourceSequence()).isEqualTo(3L);

        ObjectNode staleEpoch = envelope("EVT-STALE", 0, 8);
        addDoublePoint(staleEpoch, "flow.instant", 17.0, "t/h", "GOOD");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .header("X-Trace-Id", marker + "-TRACE-STALE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(staleEpoch)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUARANTINED"));
        assertThat(count("bpi_telemetry_quarantine")).isEqualTo(1);
        assertThat(eventExists(marker + "-EVT-STALE")).isFalse();

        ObjectNode invalidEnvelope = envelope("EVT-INVALID", 1, 4);
        invalidEnvelope.remove("eventTimeMs");
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidEnvelope)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUARANTINED"));
        assertThat(count("bpi_telemetry_quarantine")).isEqualTo(2);

        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUARANTINED"));
        assertThat(count("bpi_telemetry_quarantine")).isEqualTo(3);

        ObjectNode empty = envelope("EVT-EMPTY", 1, 4);
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(empty)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("EMPTY"))
                .andExpect(jsonPath("$.data.acceptedPoints").value(0));
        assertThat(sourceSequence()).isEqualTo(4L);

        String viewer = token(tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(envelope("EVT-NO-ROLE", 1, 5))))
                .andExpect(status().isForbidden());

        String wrongLine = token(tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + wrongLine)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(envelope("EVT-WRONG-LINE", 1, 5))))
                .andExpect(status().isForbidden());

        assertThat(count("bpi_telemetry_events")).isEqualTo(4);
        assertThat(sourceSequence()).isEqualTo(4L);
    }

    @Test
    void concurrentIdenticalEnvelopeCreatesOneFactAndReturnsOneReplay() throws Exception {
        ObjectNode envelope = envelope("EVT-CONCURRENT", 9, 1);
        addDoublePoint(envelope, "flow.instant", 22.5, "t/h", "GOOD");
        byte[] body = objectMapper.writeValueAsBytes(envelope);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentPost(body, ready, start));
            Future<Integer> second = executor.submit(() -> concurrentPost(body, ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 201);
            assertThat(count("bpi_telemetry_events")).isEqualTo(1);
            assertThat(count("bpi_telemetry_points")).isEqualTo(1);
            assertThat(sourceSequence()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private ObjectNode envelope(String eventSuffix, long sourceEpoch, long sequence) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", marker + "-" + eventSuffix);
        envelope.put("messageId", marker + "-MSG-" + eventSuffix);
        envelope.put("tenantId", tenantId);
        envelope.put("plantId", "PLANT-01");
        envelope.put("lineId", "LINE-S07-01");
        envelope.put("gatewayId", "GW-S07-01");
        envelope.put("productId", "JETLINKS-PRODUCT-SUGAR");
        envelope.put("deviceId", "FLOW-S07-01");
        envelope.put("eventTimeMs", sampleTime);
        envelope.put("ingestTimeMs", sampleTime + 1_000);
        envelope.put("sequence", sequence);
        envelope.putArray("points");
        envelope.putObject("headers").put("source", "approved-replay");
        envelope.put("sourceEpoch", sourceEpoch);
        envelope.put("sequenceOrigin", "EXPORTER");
        return envelope;
    }

    private void addDoublePoint(ObjectNode envelope, String propertyId, double value, String unit, String quality) {
        ObjectNode point = points(envelope).addObject();
        point.put("propertyId", propertyId);
        point.put("doubleValue", value);
        point.put("unit", unit);
        point.put("qualityCode", quality);
        point.put("sampleTimeMs", sampleTime);
        point.put("calibrationVersion", "CAL-1");
    }

    private void addBooleanPoint(ObjectNode envelope, String propertyId, boolean value, String unit, String quality) {
        ObjectNode point = points(envelope).addObject();
        point.put("propertyId", propertyId);
        point.put("boolValue", value);
        point.put("unit", unit);
        point.put("qualityCode", quality);
        point.put("sampleTimeMs", sampleTime);
    }

    private ArrayNode points(ObjectNode envelope) {
        return (ArrayNode) envelope.path("points");
    }

    private int concurrentPost(byte[] body, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/internal/bpi/v1/telemetry")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    private long count(String table) {
        Long result = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return result == null ? 0 : result;
    }

    private boolean eventExists(String eventId) {
        Long result = jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_telemetry_events
                 WHERE tenant_id = ? AND event_id = ?
                """, Long.class, tenantId, eventId);
        return result != null && result > 0;
    }

    private long sourceSequence() {
        return jdbc.queryForObject("""
                SELECT last_sequence::bigint FROM bpi.bpi_telemetry_source_state
                 WHERE tenant_id = ? AND gateway_id = 'GW-S07-01' AND device_id = 'FLOW-S07-01'
                """, Long.class, tenantId);
    }

    private String token(String tenant, List<String> roles, List<String> plants, List<String> lines) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject("telemetry-acceptance")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenant)
                .claim("roles", roles)
                .claim("plant_ids", plants)
                .claim("line_ids", lines)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
