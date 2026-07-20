package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.PointCatalogService;
import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.domain.PointCatalogView;
import com.mapletct.ftmes.bpi.infrastructure.pointcatalog.BpiPointCatalogKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.pointcatalog.PointCatalogKafkaListener;
import com.mapletct.ftmes.bpi.infrastructure.pointcatalog.PointCatalogKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.pointcatalog.PointCatalogKafkaRecordRejectedException;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogSnapshotCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointCatalogKafkaRecordProcessorTest {
    private static final String TOPIC = "iot.point-catalog.snapshot.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";

    private PointCatalogService service;
    private PointCatalogKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        service = mock(PointCatalogService.class);
        when(service.importSnapshot(any(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CommandResult<>(mock(PointCatalogView.class), false));
        processor = processor(Set.of(TENANT), Set.of(PLANT), Set.of(LINE));
    }

    @Test
    void validTrustedRecordUsesScopedSystemActorAndSharedSnapshotService() {
        PointCatalogSnapshotV1 event = event();

        processor.process(record(event));

        ArgumentCaptor<ActorContext> actor = ArgumentCaptor.forClass(ActorContext.class);
        ArgumentCaptor<PointCatalogSnapshotCommand> command =
                ArgumentCaptor.forClass(PointCatalogSnapshotCommand.class);
        verify(service).importSnapshot(
                actor.capture(),
                org.mockito.ArgumentMatchers.eq(event.getEventId()),
                org.mockito.ArgumentMatchers.eq("0"),
                command.capture(),
                org.mockito.ArgumentMatchers.eq(event.getEventId()));
        assertThat(actor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(actor.getValue().userId()).isEqualTo("jetlinks-point-catalog-sync");
        assertThat(actor.getValue().roles()).containsExactly("BPI_ADMIN");
        assertThat(command.getValue().sourceRevision()).isEqualTo(event.getSourceRevision());
        assertThat(command.getValue().points()).hasSize(1);
        assertThat(command.getValue().points().get(0).deviceState()).isEqualTo("ACTIVE");
        assertThat(command.getValue().points().get(0).calibrationStatus()).isEqualTo("VERIFIED");
        assertThat(command.getValue().points().get(0).sourceSequenceRequired()).isTrue();
        assertThat(command.getValue().points().get(0).sourceSequenceOrigin()).isEqualTo("DEVICE");
        assertThat(command.getValue().points().get(0).sourceSequenceBindingFingerprint())
                .isEqualTo(SourceSequenceEvidenceTestFixture.FINGERPRINT);
    }

    @Test
    void legacyContentOnlyEventIdentityRemainsAcceptedDuringRollingUpgrade() {
        PointCatalogSnapshotV1 current = event();
        PointCatalogSnapshotV1 legacy = current.toBuilder()
                .setEventId("point-catalog-" + current.getSourceRevision().substring("sha256:".length()))
                .build();

        processor.process(record(legacy));

        verify(service).importSnapshot(
                any(),
                org.mockito.ArgumentMatchers.eq(legacy.getEventId()),
                org.mockito.ArgumentMatchers.eq("0"),
                any(),
                org.mockito.ArgumentMatchers.eq(legacy.getEventId()));
    }

    @Test
    void malformedOutOfScopeAndUnspecifiedReadinessAreRejectedBeforePersistence() {
        ConsumerRecord<byte[], byte[]> malformed = new ConsumerRecord<>(
                TOPIC, 0, 1L, "x".getBytes(StandardCharsets.UTF_8), new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.process(malformed))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("Protobuf");

        PointCatalogKafkaRecordProcessor denyAll = processor(Set.of("OTHER"), Set.of(PLANT), Set.of(LINE));
        assertThatThrownBy(() -> denyAll.process(record(event())))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("outside");

        PointCatalogSnapshotV1 unspecified = event().toBuilder()
                .setPoints(0, event().getPoints(0).toBuilder()
                        .setDeviceState(PointDeviceStateV1.POINT_DEVICE_STATE_UNSPECIFIED))
                .build();
        assertThatThrownBy(() -> processor.process(record(unspecified)))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("device_state");
        verify(service, never()).importSnapshot(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void keyAndRequiredHeadersMustExactlyMatchPayload() {
        PointCatalogSnapshotV1 event = event();
        ConsumerRecord<byte[], byte[]> wrongKey = copyWithKey(record(event), "OTHER|KEY");
        assertThatThrownBy(() -> processor.process(wrongKey))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("record key");

        ConsumerRecord<byte[], byte[]> duplicateTenant = record(event);
        duplicateTenant.headers().add("tenant_id", TENANT.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(duplicateTenant))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("exactly once");

        ConsumerRecord<byte[], byte[]> wrongRevision = record(event);
        wrongRevision.headers().remove("source_revision");
        wrongRevision.headers().add("source_revision", "sha256:wrong".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(wrongRevision))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("does not match");

        PointCatalogSnapshotV1 tampered = event().toBuilder()
                .setPoints(0, event().getPoints(0).toBuilder().setUnit("t/h"))
                .build();
        assertThatThrownBy(() -> processor.process(record(tampered)))
                .isInstanceOf(PointCatalogKafkaRecordRejectedException.class)
                .hasMessageContaining("canonical payload SHA-256");
        verify(service, never()).importSnapshot(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void listenerAcknowledgesOnlyAfterSnapshotPersistenceReturns() {
        PointCatalogKafkaRecordProcessor mockedProcessor = mock(PointCatalogKafkaRecordProcessor.class);
        PointCatalogKafkaListener listener = new PointCatalogKafkaListener(mockedProcessor);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<byte[], byte[]> record = record(event());

        listener.receive(record, acknowledgment);
        verify(acknowledgment).acknowledge();

        doThrow(new IllegalStateException("database unavailable")).when(mockedProcessor).process(record);
        assertThatThrownBy(() -> listener.receive(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
    }

    private PointCatalogKafkaRecordProcessor processor(
            Set<String> tenants,
            Set<String> plants,
            Set<String> lines) {
        return new PointCatalogKafkaRecordProcessor(
                service,
                new BpiPointCatalogKafkaProperties(
                        false,
                        "localhost:29092",
                        TOPIC,
                        "iot.point-catalog.snapshot.dlq.v1",
                        "bpi-service-point-catalog-test",
                        "bpi-service-point-catalog-test",
                        "jetlinks-point-catalog-sync",
                        tenants,
                        plants,
                        lines,
                        1,
                        4,
                        Duration.ofSeconds(2),
                        5_242_880));
    }

    private static PointCatalogSnapshotV1 event() {
        PointCatalogPointV1 point = PointCatalogPointV1.newBuilder()
                .setLocalityGroup("line-01.feed")
                .setProductId("flow-product")
                .setDeviceId("meter-01")
                .setPropertyId("feed.flow")
                .setSourcePropertyId("instantFlow")
                .setPointName("Instant flow")
                .setUnit("m3/h")
                .setDataType("double")
                .setDeviceState(PointDeviceStateV1.POINT_DEVICE_ACTIVE)
                .setRegistered(true)
                .setPropertyPresent(true)
                .setCalibrationVersion("calibration-v1")
                .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                .setSourceSequenceEnabled(true)
                .setSourceSequenceRequired(true)
                .setSourceSequenceOrigin(SequenceOrigin.DEVICE)
                .setSourceSequenceBindingFingerprint(SourceSequenceEvidenceTestFixture.FINGERPRINT)
                .build();
        PointCatalogSnapshotV1 content = PointCatalogSnapshotV1.newBuilder()
                .setSource("JETLINKS")
                .setSourceInstance("jetlinks-pilot-01")
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .addPoints(point)
                .build();
        String digest = sha256(content.toByteArray());
        long observedAtMs = Instant.now().minusSeconds(1).toEpochMilli();
        return content.toBuilder()
                .setEventId("point-catalog-" + digest + "-" + observedAtMs)
                .setSourceRevision("sha256:" + digest)
                .setObservedAtMs(observedAtMs)
                .setReason("Automatic JetLinks point catalog synchronization")
                .build();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static ConsumerRecord<byte[], byte[]> record(PointCatalogSnapshotV1 event) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                TOPIC,
                0,
                1L,
                key(event).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("source_revision", event.getSourceRevision().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private static String key(PointCatalogSnapshotV1 event) {
        return String.join(
                "|", event.getTenantId(), event.getPlantId(), event.getLineId(), event.getSourceInstance());
    }

    private static ConsumerRecord<byte[], byte[]> copyWithKey(
            ConsumerRecord<byte[], byte[]> source,
            String key) {
        ConsumerRecord<byte[], byte[]> copy = new ConsumerRecord<>(
                source.topic(), source.partition(), source.offset(),
                key.getBytes(StandardCharsets.UTF_8), source.value());
        source.headers().forEach(header -> copy.headers().add(header));
        return copy;
    }
}
