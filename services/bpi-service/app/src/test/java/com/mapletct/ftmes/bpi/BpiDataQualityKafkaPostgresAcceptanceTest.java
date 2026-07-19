package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "bpi.data-quality-kafka.enabled=true",
        "bpi.data-quality-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.data-quality-kafka.allowed-tenant-ids=*",
        "bpi.data-quality-kafka.allowed-plant-ids=*",
        "bpi.data-quality-kafka.allowed-line-ids=*",
        "bpi.data-quality-kafka.concurrency=1",
        "bpi.data-quality-kafka.max-attempts=1",
        "bpi.data-quality-kafka.retry-backoff=100ms"
})
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {"bpi.data-quality.v1", "bpi.data-quality.dlq.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiDataQualityKafkaPostgresAcceptanceTest {
    private static final String SECRET = "bpi-data-quality-test-secret-0123456789";
    private static final String PLANT_ID = "PLANT-DQ-01";
    private static final String LINE_ID = "LINE-DQ-01";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired
    @Qualifier("bpiDataQualityKafkaTemplate")
    KafkaTemplate<byte[], byte[]> kafkaTemplate;

    private String tenantId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID batchId;
    private String batchNo;
    private String viewerToken;
    private String shiftToken;

    @BeforeEach
    void seedImpactContext() throws Exception {
        tenantId = "ADP_E2E_DQ_" + UUID.randomUUID().toString().replace("-", "");
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        batchId = UUID.randomUUID();
        batchNo = "ADP_E2E_DQ_BATCH_" + tenantId.substring(tenantId.length() - 8);
        viewerToken = token("dq-viewer", List.of("BPI_VIEWER"));
        shiftToken = token("dq-shift", List.of("BPI_SHIFT_LEAD"));
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition, created_by,
                     plant_id, line_id)
                VALUES (?, ?, 'TOPO-DQ', '1', 'PUBLISHED', 'topology-dq-checksum', '{}'::jsonb,
                        'acceptance', ?, ?)
                """, topologyId, tenantId, PLANT_ID, LINE_ID);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum,
                     definition, created_by, plant_id, line_id)
                VALUES (?, ?, 'RULE-DQ-START', '1.0.0', ?, 'PUBLISHED', 'rule-dq-checksum',
                        '{}'::jsonb, 'acceptance', ?, ?)
                """, ruleId, tenantId, topologyId, PLANT_ID, LINE_ID);
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_instances
                    (id, tenant_id, plant_id, batch_no, line_id, stage_code, state, revision,
                     is_shadow, start_time, quantity, quantity_unit, quality_gate, wms_status,
                     topology_version_id, rule_version_id, created_by)
                VALUES (?, ?, ?, ?, ?, 'EVAPORATION', 'ACTIVE', 1, true, ?, 0, 't',
                        'NOT_APPLICABLE', 'NOT_REQUESTED', ?, ?, 'acceptance')
                """, batchId, tenantId, PLANT_ID, batchNo, LINE_ID,
                java.sql.Timestamp.from(Instant.now().minusSeconds(600)), topologyId, ruleId);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incident_actions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incident_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
    }

    @Test
    void kafkaEventsAggregateIntoAuditedOperatorWorkflowWithoutDeletingRawFacts() throws Exception {
        Instant base = Instant.now().minusSeconds(120);
        DataQualityEventV1 first = event("0001", "CLOCK_DRIFT", DataQualitySeverity.WARNING, base);
        DataQualityEventV1 second = event("0002", "CLOCK_DRIFT", DataQualitySeverity.ERROR, base.plusSeconds(30));

        send(first);
        awaitCount("bpi_data_quality_incidents", 1);
        send(first);
        awaitStableCount("bpi_data_quality_incident_events", 1);
        send(second);
        awaitCount("bpi_data_quality_incident_events", 2);

        assertThat(count("bpi_data_quality_incidents")).isEqualTo(1);
        assertThat(count("bpi_inbox_events")).isEqualTo(2);
        assertThat(count("bpi_data_quality_incident_actions")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || severity || '|' || event_count
                  FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo("OPEN|2|ERROR|2");

        MvcResult listed = mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("state", "OPEN").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.data[0].eventCount").value(2))
                .andExpect(jsonPath("$.data[0].affectedBatchCount").value(1))
                .andExpect(jsonPath("$.data[0].affectedRules[*]").value(hasItem("RULE-DQ-START@1.0.0")))
                .andExpect(jsonPath("$.data[0].affectedBatches[*]").value(hasItem(batchNo)))
                .andReturn();
        UUID incidentId = UUID.fromString(response(listed).path("data").get(0).path("id").asText());

        mockMvc.perform(get("/bpi/v1/data-quality/summary")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(1))
                .andExpect(jsonPath("$.data.affectedBatches").value(1))
                .andExpect(jsonPath("$.data.issueCounts.CLOCK_DRIFT").value(1));
        mockMvc.perform(get("/bpi/v1/data-quality/incidents/{id}", incidentId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incident.id").value(incidentId.toString()))
                .andExpect(jsonPath("$.data.events.length()").value(2))
                .andExpect(jsonPath("$.data.lifecycle[0].action").value("CREATED"))
                .andExpect(jsonPath("$.data.recommendedActions.length()").value(3));

        byte[] acknowledgeBody = objectMapper.writeValueAsBytes(Map.of(
                "assignee", "instrument-team", "reason", "班长确认时钟漂移并分派仪表组"));
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/acknowledge", incidentId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "dq-viewer-denied-" + incidentId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(acknowledgeBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/acknowledge", incidentId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .contentType(MediaType.APPLICATION_JSON).content(acknowledgeBody))
                .andExpect(status().is(428));

        String acknowledgeKey = "dq-ack-" + incidentId;
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/acknowledge", incidentId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", acknowledgeKey).header("If-Match", "2")
                        .header("X-Trace-Id", "DQ_ACK_" + incidentId)
                        .contentType(MediaType.APPLICATION_JSON).content(acknowledgeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.assignee").value("instrument-team"));
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/acknowledge", incidentId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", acknowledgeKey).header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(acknowledgeBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(3));

        byte[] resolveBody = objectMapper.writeValueAsBytes(Map.of("reason", "NTP 校时完成并观察稳定"));
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/resolve", incidentId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "dq-resolve-stale-" + incidentId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(resolveBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("revision is stale")));
        mockMvc.perform(post("/bpi/v1/data-quality/incidents/{id}/resolve", incidentId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "dq-resolve-" + incidentId)
                        .header("If-Match", "3")
                        .header("X-Trace-Id", "DQ_RESOLVE_" + incidentId)
                        .contentType(MediaType.APPLICATION_JSON).content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RESOLVED"))
                .andExpect(jsonPath("$.data.revision").value(4));

        DataQualityEventV1 lateOld = event("0003", "CLOCK_DRIFT", DataQualitySeverity.WARNING, base.plusSeconds(10));
        send(lateOld);
        awaitIncidentRevision(5);
        assertThat(jdbc.queryForObject(
                "SELECT state FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?",
                String.class, tenantId)).isEqualTo("RESOLVED");

        DataQualityEventV1 reopened = event(
                "0004", "CLOCK_DRIFT", DataQualitySeverity.CRITICAL, Instant.now().plusSeconds(2));
        send(reopened);
        awaitIncidentRevision(6);
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || severity || '|' || event_count || '|'
                       || COALESCE(assignee, '-') || '|' || COALESCE(resolved_by, '-')
                  FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo("OPEN|CRITICAL|4|-|-");
        assertThat(count("bpi_data_quality_incident_events")).isEqualTo(4);
        assertThat(count("bpi_data_quality_incident_actions")).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_data_quality_incident_actions
                 WHERE tenant_id = ? AND action = 'REOPENED'
                """, Integer.class, tenantId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'DATA_QUALITY_INCIDENT'
                """, Integer.class, tenantId)).isEqualTo(4);

        byte[] poison = new byte[]{1, 2, 3};
        kafkaTemplate.send(new ProducerRecord<>(
                "bpi.data-quality.v1", 0,
                "invalid|data-quality".getBytes(StandardCharsets.UTF_8), poison)).get(10, TimeUnit.SECONDS);
        ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
        assertThat(dlq.value()).containsExactly(poison);
    }

    @Test
    void incidentQueueUsesSignedScopeBoundSnapshotCutoffPagination() throws Exception {
        Instant base = Instant.now().minusSeconds(90);
        send(event("PAGE-1", "REQUIRED_SIGNAL_UNAVAILABLE", DataQualitySeverity.CRITICAL, base));
        send(event("PAGE-2", "CLOCK_DRIFT", DataQualitySeverity.ERROR, base.plusSeconds(1)));
        send(event("PAGE-3", "UNKNOWN_UNIT", DataQualitySeverity.WARNING, base.plusSeconds(2)));
        awaitCount("bpi_data_quality_incidents", 3);

        MvcResult firstResult = mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].issueCode").value("REQUIRED_SIGNAL_UNAVAILABLE"))
                .andExpect(jsonPath("$.data[1].issueCode").value("CLOCK_DRIFT"))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andReturn();
        JsonNode first = response(firstResult);
        String nextCursor = first.path("meta").path("nextCursor").asText();

        MvcResult secondResult = mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("limit", "2").param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].issueCode").value("UNKNOWN_UNIT"))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andReturn();
        assertThat(first.path("data").findValuesAsText("id"))
                .doesNotContainAnyElementsOf(response(secondResult).path("data").findValuesAsText("id"));

        mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("cursor", nextCursor).param("search", "other"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("does not match")));
        String tamperedCursor = (nextCursor.startsWith("A") ? "B" : "A") + nextCursor.substring(1);
        mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("cursor", tamperedCursor))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("cursor is invalid")));
        mockMvc.perform(get("/bpi/v1/data-quality/incidents")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID).param("limit", "201"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("between 1 and 200")));
    }

    private DataQualityEventV1 event(
            String suffix,
            String issueCode,
            DataQualitySeverity severity,
            Instant detectedAt) {
        return DataQualityEventV1.newBuilder()
                .setEventId("DQ-E2E-" + tenantId.substring(tenantId.length() - 12) + "-" + suffix)
                .setSourceEventId("SOURCE-E2E-" + suffix)
                .setTenantId(tenantId).setPlantId(PLANT_ID).setLineId(LINE_ID)
                .setDeviceId("FLOW-METER-01").setPropertyId("flow.instant")
                .setIssueCode(issueCode).setSeverity(severity)
                .setDetail("ADP_E2E data-quality marker " + suffix)
                .setDetectedAtMs(detectedAt.toEpochMilli())
                .putHeaders("stage", "boundary-evaluation")
                .putHeaders("rule_key", String.join(
                        "|", tenantId, PLANT_ID, LINE_ID, "RULE-DQ-START", "1.0.0"))
                .build();
    }

    private void send(DataQualityEventV1 event) throws Exception {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                "bpi.data-quality.v1", null, event.getDetectedAtMs(),
                key(event).getBytes(StandardCharsets.UTF_8), event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("issue_code", event.getIssueCode().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    }

    private String key(DataQualityEventV1 event) {
        return String.join("|", event.getTenantId(), event.getLineId(),
                event.getSourceEventId(), event.getPropertyId(), event.getIssueCode());
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String token(String subject, List<String> roles) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter").audience("bpi-service").subject(subject)
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenantId).claim("roles", roles)
                .claim("plant_ids", List.of(PLANT_ID)).claim("line_ids", List.of(LINE_ID))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "data-quality-dlq-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of("bpi.data-quality.dlq.v1"));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<byte[], byte[]> record : records) return record;
            }
        }
        throw new AssertionError("No data-quality DLQ record arrived before timeout");
    }

    private void awaitCount(String table, long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (count(table) == expected) return;
            Thread.sleep(100);
        }
        assertThat(count(table)).isEqualTo(expected);
    }

    private void awaitStableCount(String table, long expected) throws InterruptedException {
        awaitCount(table, expected);
        Thread.sleep(500);
        assertThat(count(table)).isEqualTo(expected);
    }

    private void awaitIncidentRevision(long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            Long revision = jdbc.queryForObject(
                    "SELECT revision FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?",
                    Long.class, tenantId);
            if (revision != null && revision == expected) return;
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject(
                "SELECT revision FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?",
                Long.class, tenantId)).isEqualTo(expected);
    }

    private long count(String table) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return count == null ? 0 : count;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
