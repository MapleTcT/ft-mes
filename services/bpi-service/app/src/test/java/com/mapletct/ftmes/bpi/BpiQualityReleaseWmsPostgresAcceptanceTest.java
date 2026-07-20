package com.mapletct.ftmes.bpi;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionDispositionV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionResultV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundStatusV1;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import com.mapletct.ftmes.bpi.infrastructure.integration.WmsInboundOutboxRepository;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiQualityReleaseWmsPostgresAcceptanceTest {
    private static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");
    private static final String SECRET = "bpi-phase2-test-secret-0123456789";
    private static final String PLANT_ID = "PLANT-QW-01";
    private static final String LINE_ID = "LINE-QW-01";
    private static final String MATERIAL_CODE = "SUGAR-FG-001";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
        registry.add("bpi.phase2-integration.enabled", () -> true);
        registry.add("bpi.phase2-integration.protobuf-http-ingress-enabled", () -> true);
        registry.add("bpi.phase2-integration.allowed-tenant-ids", () -> "*");
        registry.add("bpi.phase2-integration.allowed-plant-ids", () -> "*");
        registry.add("bpi.phase2-integration.allowed-line-ids", () -> "*");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired WmsInboundOutboxRepository wmsOutboxRepository;
    @Autowired RulePublicationOutboxRepository ruleOutboxRepository;

    private String tenantId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID batchId;
    private String integrationToken;
    private String viewerToken;

    @BeforeEach
    void seedClosedRawBatchAndPhase2Flags() throws Exception {
        tenantId = "ADP_E2E_20260720_BPI_QW_" + UUID.randomUUID().toString().replace("-", "");
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition, created_by)
                VALUES (?, ?, 'TOPO-QW', '1', 'PUBLISHED', 'topology-qw-checksum', '{}'::jsonb, 'acceptance')
                """, topologyId, tenantId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
                VALUES (?, ?, 'RULE-QW-END', '1', ?, 'PUBLISHED', 'rule-qw-checksum', '{}'::jsonb, 'acceptance')
                """, ruleId, tenantId, topologyId);
        seedFlag("bpi.qcs-link", true);
        seedFlag("bpi.wms-link", true);
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_instances
                    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
                     material_code, state, revision, is_shadow, start_time, end_time,
                     quantity, quantity_unit, quality_gate, wms_status,
                     topology_version_id, rule_version_id, created_by)
                VALUES (?, ?, ?, ?, ?, 'PACKING', ?, ?, 'CLOSED_RAW', 1, false,
                        now() - interval '2 hours', now() - interval '1 hour',
                        12.345000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED', ?, ?, 'acceptance')
                """, batchId, tenantId, PLANT_ID, "ADP_E2E_BATCH_" + batchId,
                LINE_ID, "ADP_E2E_ORDER_" + batchId, MATERIAL_CODE, topologyId, ruleId);
        integrationToken = token(List.of("BPI_INTEGRATION_INGEST"));
        viewerToken = token(List.of("BPI_VIEWER"));
        System.out.printf("BPI_PHASE2_ACCEPTANCE_MARKER tenant=%s batchId=%s%n", tenantId, batchId);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_quality_links WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_wms_inbound_links WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_quality_gates WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_state_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        long residual = count("bpi_quality_links")
                + count("bpi_wms_inbound_links")
                + count("bpi_quality_gates")
                + count("bpi_outbox_events")
                + count("bpi_audit_events")
                + count("bpi_batch_state_events")
                + count("bpi_api_idempotency")
                + count("bpi_inbox_events")
                + count("bpi_batch_instances")
                + count("bpi_feature_flags")
                + count("bpi_rule_versions")
                + count("bpi_topology_versions");
        assertThat(residual).isZero();
        System.out.printf("BPI_PHASE2_ACCEPTANCE_CLEANUP tenant=%s residualRows=%d%n", tenantId, residual);
    }

    @Test
    void qcsPendingThenAcceptedPublishesOneWmsCommandAndAcceptedReceiptPersistsInboundDocument()
            throws Exception {
        String gateId = "ADP_E2E_GATE_" + batchId;
        QcsQualityGateV1 pending = qualityGate(
                gateId, 1, "QCS-PENDING-" + batchId,
                QcsInspectionDispositionV1.QCS_INSPECTION_PENDING, false);
        postQcs(pending)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("WAIT_QA"))
                .andExpect(jsonPath("$.data.batch.qualityGate").value("WAITING"))
                .andExpect(jsonPath("$.data.qualityGate.externalRevision").value(1))
                .andExpect(jsonPath("$.data.qualityGate.inspections[0].disposition").value("PENDING"));

        postQcs(pending)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("WAIT_QA"));
        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(count("bpi_quality_gates")).isEqualTo(1);
        assertThat(count("bpi_quality_links")).isEqualTo(1);
        assertThat(count("bpi_batch_state_events")).isEqualTo(1);

        QcsQualityGateV1 changedReplay = pending.toBuilder()
                .setQuantityUnit("kg")
                .build();
        postQcs(changedReplay)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("replayed")));
        assertThat(count("bpi_inbox_events")).isEqualTo(1);

        QcsQualityGateV1 accepted = qualityGate(
                gateId, 2, "QCS-ACCEPTED-" + batchId,
                QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED, true);
        postQcs(accepted)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("RELEASED"))
                .andExpect(jsonPath("$.data.batch.wmsStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.qualityGate.state").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.wmsInbound.status").value("PENDING"));

        assertThat(count("bpi_outbox_events")).isEqualTo(1);
        assertThat(count("bpi_wms_inbound_links")).isEqualTo(1);
        assertThat(count("bpi_inbox_events")).isEqualTo(2);
        byte[] payload = jdbc.queryForObject("""
                SELECT payload FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND event_type = 'WMS_COMPLETION_INBOUND_COMMAND'
                """, byte[].class, tenantId);
        WmsCompletionInboundCommandV1 command = WmsCompletionInboundCommandV1.parseFrom(payload);
        UUID commandEventId = UUID.fromString(command.getEventId());
        assertThat(command.getBatchId()).isEqualTo(batchId.toString());
        assertThat(command.getMaterialCode()).isEqualTo(MATERIAL_CODE);
        assertThat(command.getQuantityDecimal()).isEqualTo("12.345");
        assertThat(command.getQualityGateId()).isEqualTo(gateId);
        assertThat(command.getQualityGateRevision()).isEqualTo(2);
        assertThat(command.getIdempotencyKey()).contains(batchId.toString()).endsWith("|2");

        assertThat(ruleOutboxRepository.claimPending(10, Duration.ofMinutes(1))).isEmpty();
        assertThat(outboxStatus(commandEventId)).isEqualTo("PENDING");

        WmsCompletionInboundReceiptV1 receipt = receipt(
                commandEventId, "WMS-ACCEPTED-" + batchId,
                WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_ACCEPTED,
                "WMS-IN-ADP-E2E-" + batchId, "");
        postWmsReceipt(receipt)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("publication")));
        assertThat(count("bpi_inbox_events")).isEqualTo(2);

        List<OutboxEventClaim> claims = wmsOutboxRepository.claimPending(10, Duration.ofMinutes(1));
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).id()).isEqualTo(commandEventId);
        assertThat(WmsCompletionInboundCommandV1.parseFrom(claims.get(0).payload()).getBatchId())
                .isEqualTo(batchId.toString());
        assertThat(wmsOutboxRepository.markPublished(commandEventId, claims.get(0).claimToken())).isTrue();

        postWmsReceipt(receipt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("INBOUNDED"))
                .andExpect(jsonPath("$.data.batch.wmsStatus").value("INBOUNDED"))
                .andExpect(jsonPath("$.data.wmsInbound.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.wmsInbound.documentId").value("WMS-IN-ADP-E2E-" + batchId));
        postWmsReceipt(receipt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("INBOUNDED"));

        mockMvc.perform(get("/bpi/v1/batches/{id}/release", batchId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qualityGate.externalRevision").value(2))
                .andExpect(jsonPath("$.data.wmsInbound.documentId").value("WMS-IN-ADP-E2E-" + batchId));
        assertThat(count("bpi_inbox_events")).isEqualTo(3);
        assertThat(count("bpi_batch_state_events")).isEqualTo(3);
        assertThat(count("bpi_audit_events")).isEqualTo(3);
        assertThat(batchProjection()).isEqualTo("INBOUNDED|4|ACCEPTED|INBOUNDED");
    }

    @Test
    void rejectedRequiredInspectionRejectsBatchWithoutCreatingWmsCommand() throws Exception {
        postQcs(qualityGate(
                "ADP_E2E_GATE_REJECT_" + batchId, 1, "QCS-REJECTED-" + batchId,
                QcsInspectionDispositionV1.QCS_INSPECTION_REJECTED, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("REJECTED"))
                .andExpect(jsonPath("$.data.batch.qualityGate").value("REJECTED"))
                .andExpect(jsonPath("$.data.wmsInbound").doesNotExist());

        assertThat(batchProjection()).isEqualTo("REJECTED|3|REJECTED|NOT_REQUESTED");
        assertThat(count("bpi_batch_state_events")).isEqualTo(2);
        assertThat(count("bpi_audit_events")).isEqualTo(2);
        assertThat(count("bpi_outbox_events")).isZero();
        assertThat(count("bpi_wms_inbound_links")).isZero();
    }

    @Test
    void qcsGateIsFailClosedAndShadowBatchCannotCreateWmsCommand() throws Exception {
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ? AND flag_key = 'bpi.qcs-link'", tenantId);
        QcsQualityGateV1 accepted = qualityGate(
                "ADP_E2E_GATE_SHADOW_" + batchId, 1, "QCS-SHADOW-" + batchId,
                QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED, true);
        postQcs(accepted)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("disabled")));
        assertThat(count("bpi_inbox_events")).isZero();
        assertThat(count("bpi_quality_gates")).isZero();
        assertThat(batchProjection()).isEqualTo("CLOSED_RAW|1|NOT_APPLICABLE|NOT_REQUESTED");

        seedFlag("bpi.qcs-link", true);
        jdbc.update("UPDATE bpi.bpi_batch_instances SET is_shadow = true WHERE tenant_id = ? AND id = ?",
                tenantId, batchId);
        postQcs(accepted)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("RELEASED"))
                .andExpect(jsonPath("$.data.batch.shadow").value(true))
                .andExpect(jsonPath("$.data.batch.wmsStatus").value("NOT_REQUESTED"))
                .andExpect(jsonPath("$.data.wmsInbound").doesNotExist());
        assertThat(count("bpi_outbox_events")).isZero();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers)
                VALUES (?, ?, ?, ?, 'BATCH_INSTANCE', ?, 'WMS_COMPLETION_INBOUND_COMMAND',
                        'bpi.wms.completion-inbound-command.v1', ?, ?, '{}'::jsonb)
                """, UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID, batchId,
                tenantId + "|" + batchId, new byte[] {1}))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("non-shadow batch");
        assertThat(count("bpi_outbox_events")).isZero();
    }

    @Test
    void rejectedWmsReceiptPersistsFailureWithoutFalselyMarkingBatchInbound() throws Exception {
        postQcs(qualityGate(
                "ADP_E2E_GATE_WMS_REJECT_" + batchId, 1, "QCS-WMS-READY-" + batchId,
                QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("RELEASED"));
        List<OutboxEventClaim> claims = wmsOutboxRepository.claimPending(10, Duration.ofMinutes(1));
        assertThat(claims).hasSize(1);
        OutboxEventClaim claim = claims.get(0);
        assertThat(wmsOutboxRepository.markPublished(claim.id(), claim.claimToken())).isTrue();

        WmsCompletionInboundReceiptV1 unknownStatus = receipt(
                claim.id(), "WMS-UNKNOWN-" + batchId,
                WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_REJECTED,
                "", "WMS_UNKNOWN_STATUS").toBuilder()
                .setStatusValue(99)
                .build();
        postWmsReceipt(unknownStatus)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.Matchers.containsString("ACCEPTED or REJECTED")));
        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(batchProjection()).isEqualTo("RELEASED|3|ACCEPTED|PENDING");
        assertThat(jdbc.queryForObject("""
                SELECT status
                  FROM bpi.bpi_wms_inbound_links
                 WHERE tenant_id = ? AND batch_id = ?
                """, String.class, tenantId, batchId)).isEqualTo("PENDING");

        postWmsReceipt(receipt(
                claim.id(), "WMS-REJECTED-" + batchId,
                WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_REJECTED,
                "", "WMS_STORAGE_LOCATION_UNAVAILABLE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batch.state").value("RELEASED"))
                .andExpect(jsonPath("$.data.batch.wmsStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.wmsInbound.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.wmsInbound.errorCode").value("WMS_STORAGE_LOCATION_UNAVAILABLE"));

        assertThat(batchProjection()).isEqualTo("RELEASED|4|ACCEPTED|FAILED");
        assertThat(jdbc.queryForObject("""
                SELECT status || '|' || error_code
                  FROM bpi.bpi_wms_inbound_links
                 WHERE tenant_id = ? AND batch_id = ?
                """, String.class, tenantId, batchId))
                .isEqualTo("REJECTED|WMS_STORAGE_LOCATION_UNAVAILABLE");
    }

    private ResultActions postQcs(QcsQualityGateV1 event) throws Exception {
        return mockMvc.perform(post("/internal/bpi/v1/qcs-quality-gates")
                .header("Authorization", "Bearer " + integrationToken)
                .contentType(PROTOBUF)
                .content(event.toByteArray()));
    }

    private ResultActions postWmsReceipt(WmsCompletionInboundReceiptV1 event) throws Exception {
        return mockMvc.perform(post("/internal/bpi/v1/wms-inbound-receipts")
                .header("Authorization", "Bearer " + integrationToken)
                .contentType(PROTOBUF)
                .content(event.toByteArray()));
    }

    private QcsQualityGateV1 qualityGate(
            String gateId,
            long revision,
            String eventId,
            QcsInspectionDispositionV1 disposition,
            boolean finalResult) {
        Instant observedAt = Instant.now().minusSeconds(60);
        QcsQualityGateV1.Builder event = QcsQualityGateV1.newBuilder()
                .setEventId(eventId)
                .setIdempotencyKey("IDEMPOTENCY-" + eventId)
                .setTenantId(tenantId)
                .setPlantId(PLANT_ID)
                .setLineId(LINE_ID)
                .setBatchId(batchId.toString())
                .setQualityGateId(gateId)
                .setQualityGateRevision(revision)
                .setMaterialCode(MATERIAL_CODE)
                .setObservedAtMs(observedAt.toEpochMilli())
                .putHeaders("trace_id", "TRACE-" + eventId)
                .addInspections(QcsInspectionResultV1.newBuilder()
                        .setInspectionCode("FG-RELEASE")
                        .setInspectionRecordId("ADP_E2E_INSPECTION_" + revision + "_" + batchId)
                        .setRequired(true)
                        .setDisposition(disposition)
                        .setFinalResult(finalResult)
                        .setObservedAtMs(observedAt.toEpochMilli()));
        if (disposition == QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED && finalResult) {
            event.setReleaseQuantityDecimal("12.345")
                    .setQuantityUnit("t");
        }
        return event.build();
    }

    private WmsCompletionInboundReceiptV1 receipt(
            UUID commandEventId,
            String eventId,
            WmsCompletionInboundStatusV1 status,
            String documentId,
            String errorCode) {
        return WmsCompletionInboundReceiptV1.newBuilder()
                .setEventId(eventId)
                .setIdempotencyKey("IDEMPOTENCY-" + eventId)
                .setCommandEventId(commandEventId.toString())
                .setTenantId(tenantId)
                .setPlantId(PLANT_ID)
                .setLineId(LINE_ID)
                .setBatchId(batchId.toString())
                .setStatus(status)
                .setDocumentId(documentId)
                .setErrorCode(errorCode)
                .setDetail(status == WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_ACCEPTED
                        ? "WMS durable document committed" : "WMS rejected the inbound request")
                .setObservedAtMs(Instant.now().toEpochMilli())
                .putHeaders("trace_id", "TRACE-" + eventId)
                .build();
    }

    private void seedFlag(String flagKey, boolean enabled) {
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', ?, ?, ?, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId, LINE_ID, flagKey, enabled);
    }

    private long count(String table) {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return value == null ? 0L : value;
    }

    private String batchProjection() {
        return jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || quality_gate || '|' || wms_status
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, batchId);
    }

    private String outboxStatus(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT status FROM bpi.bpi_outbox_events WHERE tenant_id = ? AND id = ?",
                String.class, tenantId, eventId);
    }

    private String token(List<String> roles) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject("phase2-acceptance-user")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .claim("plant_ids", List.of(PLANT_ID))
                .claim("line_ids", List.of(LINE_ID))
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
