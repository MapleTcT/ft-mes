package com.mapletct.ftmes.bpi.contract.validation;

import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BpiContractValidatorTest {

    @Test
    public void acceptsCompleteActiveProductionContext() {
        ProductionContextEventV1 context = ProductionContextEventV1.newBuilder()
            .setEventId("wom-context:1000:PLANT-01:LINE-01:7")
            .setTenantId("1000")
            .setPlantId("PLANT-01")
            .setLineId("LINE-01")
            .setOrderId("MO-001")
            .setTaskId("42")
            .setMaterialCode("MAT-001")
            .setRecipeVersion("FORMULA-01:V2")
            .setEffectiveFromMs(1_725_000_000_000L)
            .setContextRevision(7L)
            .setActive(true)
            .build();

        assertTrue(BpiContractValidator.validate(context).isEmpty());
    }

    @Test
    public void rejectsActiveProductionContextWithoutBusinessIdentity() {
        ProductionContextEventV1 context = ProductionContextEventV1.newBuilder()
            .setEventId("event-1")
            .setTenantId("1000")
            .setPlantId("PLANT-01")
            .setLineId("LINE-01")
            .setEffectiveFromMs(1_725_000_000_000L)
            .setContextRevision(1L)
            .setActive(true)
            .build();

        List<ContractViolation> violations = BpiContractValidator.validate(context);

        assertTrue(containsPath(violations, "order_id"));
        assertTrue(containsPath(violations, "task_id"));
        assertTrue(containsPath(violations, "material_code"));
        assertTrue(containsPath(violations, "recipe_version"));
    }

    @Test
    public void oneBadPointDoesNotSuppressValidPoints() throws Exception {
        TelemetryEnvelopeV1 envelope = validEnvelope().toBuilder()
            .addPoints(PointValue.newBuilder()
                .setPropertyId("flow_instant")
                .setDoubleValue(18.5d)
                .setUnit("m3/h")
                .setQualityCode("GOOD")
                .setSampleTimeMs(1_720_000_000_000L))
            .addPoints(PointValue.newBuilder()
                .setPropertyId("baume")
                .setDoubleValue(20.3d)
                .setUnit("Be")
                .setQualityCode("NOT_A_CONTROLLED_CODE")
                .setSampleTimeMs(1_720_000_000_000L))
            .build();

        TelemetryEnvelopeValidationResult result = BpiContractValidator.validate(envelope);

        assertTrue(result.isEnvelopeAccepted());
        assertEquals(1, result.getAcceptedPointIndexes().size());
        assertEquals(Integer.valueOf(0), result.getAcceptedPointIndexes().get(0));
        assertEquals(1, result.getPointRejections().size());
        assertEquals(1, result.getPointRejections().get(0).getPointIndex());
        assertEquals("UNKNOWN_QUALITY", result.getPointRejections().get(0).getViolations().get(0).getCode());
    }

    @Test
    public void envelopeIdentityFailureQuarantinesTheWholeEnvelope() {
        TelemetryEnvelopeV1 envelope = validEnvelope().toBuilder()
            .clearDeviceId()
            .addPoints(validPoint())
            .build();

        TelemetryEnvelopeValidationResult result = BpiContractValidator.validate(envelope);

        assertFalse(result.isEnvelopeAccepted());
        assertTrue(result.getAcceptedPointIndexes().isEmpty());
        assertTrue(result.getPointRejections().isEmpty());
        assertEquals("device_id", result.getEnvelopeViolations().get(0).getPath());
    }

    @Test
    public void protobufRoundTripPreservesReplayIdentityAndPointValues() throws Exception {
        TelemetryEnvelopeV1 original = validEnvelope().toBuilder()
            .addPoints(validPoint())
            .putHeaders("payload_checksum", "sha256:abc")
            .build();

        TelemetryEnvelopeV1 restored = TelemetryEnvelopeV1.parseFrom(original.toByteArray());

        assertEquals(original, restored);
        assertEquals("EVENT_1", restored.getEventId());
        assertEquals(18.5d, restored.getPoints(0).getDoubleValue(), 0.0d);
    }

    @Test
    public void emptyPointSetAndZeroBasedUnsignedIdentityRemainValidFacts() throws Exception {
        TelemetryEnvelopeV1 envelope = validEnvelope().toBuilder()
            .setSequence(0L)
            .setSourceEpoch(0L)
            .clearPoints()
            .build();

        TelemetryEnvelopeValidationResult result = BpiContractValidator.validate(envelope);

        assertTrue(result.isEnvelopeAccepted());
        assertTrue(result.getAcceptedPointIndexes().isEmpty());
        assertTrue(result.getPointRejections().isEmpty());
    }

    @Test
    public void unsigned64MaximumSurvivesProtobufRoundTrip() throws Exception {
        TelemetryEnvelopeV1 original = validEnvelope().toBuilder()
            .setSequence(-1L)
            .setSourceEpoch(-1L)
            .build();

        TelemetryEnvelopeV1 restored = TelemetryEnvelopeV1.parseFrom(original.toByteArray());

        assertEquals("18446744073709551615", Long.toUnsignedString(restored.getSequence()));
        assertEquals("18446744073709551615", Long.toUnsignedString(restored.getSourceEpoch()));
        assertTrue(BpiContractValidator.validate(restored).isEnvelopeAccepted());
    }

    @Test
    public void deterministicStartCandidateIsAccepted() {
        String candidateKey = CandidateKeyFactory.startKey(
            "TENANT_A", "LINE_01", "RULE_1.0.0", "ORDER_42", "EVENT_START_7"
        );
        BatchCandidateV1 candidate = BatchCandidateV1.newBuilder()
            .setEventId("CANDIDATE_EVENT_1")
            .setCandidateKey(candidateKey)
            .setTenantId("TENANT_A")
            .setPlantId("PLANT_01")
            .setLineId("LINE_01")
            .setBoundaryType(BoundaryType.START)
            .setRuleCode("SUGAR_BATCH_START")
            .setRuleVersion("RULE_1.0.0")
            .setTopologyVersion("TOPOLOGY_1")
            .setContextOrderId("ORDER_42")
            .setFirstQuorumEvidenceEventId("EVENT_START_7")
            .setBoundaryEventTimeMs(1_720_000_000_000L)
            .setConfidence(0.92d)
            .addEvidenceEventIds("EVENT_START_7")
            .setEmittedAtMs(1_720_000_000_500L)
            .build();

        assertTrue(BpiContractValidator.validate(candidate).isEmpty());
    }

    @Test
    public void nonDeterministicCandidateIdentityIsRejected() {
        BatchCandidateV1 candidate = validEndCandidate().toBuilder()
            .setCandidateKey("f47ac10b-58cc-4372-a567-0e02b2c3d479")
            .build();

        List<ContractViolation> violations = BpiContractValidator.validate(candidate);

        assertTrue(containsCode(violations, "NON_DETERMINISTIC_ID"));
    }

    @Test
    public void richCandidateEvidenceMustMatchTheStableEventIndex() {
        BatchCandidateV1 candidate = validEndCandidate().toBuilder()
            .addEvidence(CandidateEvidenceV1.newBuilder()
                .setEventId("EVENT_END_8")
                .setSignal("feed.flow")
                .setClassification("QUORUM")
                .setSatisfied(true)
                .setValue("0.1")
                .setUnit("t/h")
                .setQualityCode("GOOD")
                .setEventTimeMs(1_720_000_100_000L)
                .setSource("bpi-stream-engine"))
            .build();

        assertTrue(BpiContractValidator.validate(candidate).isEmpty());

        BatchCandidateV1 mismatched = candidate.toBuilder()
            .setEvidence(0, candidate.getEvidence(0).toBuilder().setEventId("NOT_INDEXED"))
            .build();
        assertTrue(containsCode(BpiContractValidator.validate(mismatched), "EVIDENCE_INDEX_MISMATCH"));
        assertTrue(containsCode(BpiContractValidator.validate(mismatched), "QUORUM_EVIDENCE_DETAIL_MISSING"));
    }

    private static TelemetryEnvelopeV1 validEnvelope() {
        return TelemetryEnvelopeV1.newBuilder()
            .setEventId("EVENT_1")
            .setMessageId("MQTT_1")
            .setTenantId("TENANT_A")
            .setPlantId("PLANT_01")
            .setLineId("LINE_01")
            .setGatewayId("GATEWAY_01")
            .setProductId("PRODUCT_01")
            .setDeviceId("DEVICE_01")
            .setEventTimeMs(1_720_000_000_000L)
            .setIngestTimeMs(1_720_000_000_050L)
            .setSequence(42L)
            .setSourceEpoch(3L)
            .setSequenceOrigin(SequenceOrigin.GATEWAY)
            .build();
    }

    private static PointValue validPoint() {
        return PointValue.newBuilder()
            .setPropertyId("flow_instant")
            .setDoubleValue(18.5d)
            .setUnit("m3/h")
            .setQualityCode("GOOD")
            .setSampleTimeMs(1_720_000_000_000L)
            .setCalibrationVersion("CAL_2026_01")
            .build();
    }

    private static BatchCandidateV1 validEndCandidate() {
        return BatchCandidateV1.newBuilder()
            .setEventId("CANDIDATE_EVENT_END")
            .setCandidateKey(CandidateKeyFactory.endKey("BATCH_99", "RULE_1.0.0", "EVENT_END_8"))
            .setTenantId("TENANT_A")
            .setPlantId("PLANT_01")
            .setLineId("LINE_01")
            .setBoundaryType(BoundaryType.END)
            .setRuleCode("SUGAR_BATCH_END")
            .setRuleVersion("RULE_1.0.0")
            .setTopologyVersion("TOPOLOGY_1")
            .setBatchId("BATCH_99")
            .setFirstQuorumEvidenceEventId("EVENT_END_8")
            .setBoundaryEventTimeMs(1_720_000_100_000L)
            .setConfidence(0.9d)
            .addEvidenceEventIds("EVENT_END_8")
            .setEmittedAtMs(1_720_000_100_500L)
            .build();
    }

    private static boolean containsCode(List<ContractViolation> violations, String code) {
        for (ContractViolation violation : violations) {
            if (code.equals(violation.getCode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPath(List<ContractViolation> violations, String path) {
        for (ContractViolation violation : violations) {
            if (path.equals(violation.getPath())) {
                return true;
            }
        }
        return false;
    }
}
