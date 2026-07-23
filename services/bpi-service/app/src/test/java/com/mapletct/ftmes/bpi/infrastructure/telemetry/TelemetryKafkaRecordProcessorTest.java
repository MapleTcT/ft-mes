package com.mapletct.ftmes.bpi.infrastructure.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.TelemetryIngestionService;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import com.mapletct.ftmes.bpi.domain.TelemetryIngestResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryKafkaRecordProcessorTest {
    private static final String TOPIC = "iot.telemetry.selected.v1";
    private static final String TENANT = "tenant-a";
    private static final String PLANT = "plant-a";
    private static final String LINE = "line-a";
    private static final String DEVICE = "flow-meter-a";

    @Test
    void validRecordUsesScopedActorAndPreservesUnsignedSequenceAndPointSemantics() {
        TelemetryIngestionService service = mock(TelemetryIngestionService.class);
        when(service.ingestKafka(any(), any(), anyString())).thenReturn(
                new TelemetryIngestResult("event-a", "ACCEPTED", "FIRST", 1, 0, false, "trace"));
        TelemetryKafkaRecordProcessor processor = processor(service, Set.of("*"), Set.of("*"), Set.of("*"));
        TelemetryEnvelopeV1 event = event().toBuilder()
                .setSourceEpoch(-1L)
                .setSequence(-1L)
                .build();

        TelemetryIngestResult result = processor.process(record(TOPIC, PLANT + "|" + DEVICE, event));

        ArgumentCaptor<ActorContext> actor = ArgumentCaptor.forClass(ActorContext.class);
        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(service).ingestKafka(actor.capture(), payload.capture(), anyString());
        assertThat(actor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(actor.getValue().roles()).containsExactly("BPI_EVENT_INGEST");
        assertThat(actor.getValue().plantIds()).containsExactly(PLANT);
        assertThat(actor.getValue().lineIds()).containsExactly(LINE);
        assertThat(payload.getValue().path("sourceEpoch").bigIntegerValue().toString())
                .isEqualTo("18446744073709551615");
        assertThat(payload.getValue().path("sequence").bigIntegerValue().toString())
                .isEqualTo("18446744073709551615");
        assertThat(payload.getValue().path("sequenceOrigin").asText()).isEqualTo("DEVICE");
        assertThat(payload.getValue().path("points").get(0).path("doubleValue").asDouble()).isEqualTo(12.5d);
        assertThat(result.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void rejectsUntrustedTopicScopeKeyAndPointContract() {
        TelemetryIngestionService service = mock(TelemetryIngestionService.class);
        TelemetryKafkaRecordProcessor processor = processor(
                service, Set.of(TENANT), Set.of(PLANT), Set.of(LINE));
        TelemetryEnvelopeV1 event = event();

        assertThatThrownBy(() -> processor.decodeAndValidate(record(
                "other.telemetry.v1", PLANT + "|" + DEVICE, event)))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("untrusted topic");
        assertThatThrownBy(() -> processor.decodeAndValidate(record(
                TOPIC, PLANT + "|wrong-device", event)))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("plant_id|device_id");

        TelemetryKafkaRecordProcessor denied = processor(
                service, Set.of("other-tenant"), Set.of(PLANT), Set.of(LINE));
        assertThatThrownBy(() -> denied.decodeAndValidate(record(TOPIC, PLANT + "|" + DEVICE, event)))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("outside the configured");

        TelemetryEnvelopeV1 badPoint = event.toBuilder()
                .clearPoints()
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(12.5d)
                        .setUnit("m3/h")
                        .setQualityCode("NOT_CONTROLLED")
                        .setSampleTimeMs(Instant.now().toEpochMilli()))
                .build();
        assertThatThrownBy(() -> processor.decodeAndValidate(
                record(TOPIC, PLANT + "|" + DEVICE, badPoint)))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("UNKNOWN_QUALITY");
    }

    @Test
    void rejectsNullOversizedAndMalformedPayloads() {
        TelemetryIngestionService service = mock(TelemetryIngestionService.class);
        TelemetryKafkaRecordProcessor processor = processor(service, Set.of("*"), Set.of("*"), Set.of("*"));

        ConsumerRecord<byte[], byte[]> tombstone =
                new ConsumerRecord<>(TOPIC, 0, 1L, bytes(PLANT + "|" + DEVICE), null);
        assertThatThrownBy(() -> processor.decodeAndValidate(tombstone))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("payload size");

        ConsumerRecord<byte[], byte[]> malformed =
                new ConsumerRecord<>(TOPIC, 0, 2L, bytes(PLANT + "|" + DEVICE), new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.decodeAndValidate(malformed))
                .isInstanceOf(TelemetryKafkaRecordRejectedException.class)
                .hasMessageContaining("not valid");
    }

    private TelemetryKafkaRecordProcessor processor(
            TelemetryIngestionService service,
            Set<String> tenants,
            Set<String> plants,
            Set<String> lines) {
        BpiTelemetryKafkaProperties properties = new BpiTelemetryKafkaProperties(
                true,
                "localhost:9092",
                TOPIC,
                "iot.telemetry.selected.dlq.v1",
                "test-group",
                "test-client",
                "jetlinks-test",
                tenants,
                plants,
                lines,
                1,
                2,
                Duration.ofMillis(100),
                524_288,
                "latest");
        return new TelemetryKafkaRecordProcessor(service, properties, new ObjectMapper());
    }

    private TelemetryEnvelopeV1 event() {
        long now = Instant.now().toEpochMilli();
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId("event-a")
                .setMessageId("message-a")
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setGatewayId("gateway-a")
                .setProductId("flow-product")
                .setDeviceId(DEVICE)
                .setEventTimeMs(now)
                .setIngestTimeMs(now)
                .setSourceEpoch(1L)
                .setSequence(1L)
                .setSequenceOrigin(SequenceOrigin.DEVICE)
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("feed.flow")
                        .setDoubleValue(12.5d)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(now)
                        .setCalibrationVersion("CAL-1"))
                .putHeaders("locality_group", "line-a.feed")
                .build();
    }

    private ConsumerRecord<byte[], byte[]> record(String topic, String key, TelemetryEnvelopeV1 event) {
        return new ConsumerRecord<>(topic, 0, 0L, bytes(key), event.toByteArray());
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
