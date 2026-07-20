package com.mapletct.ftmes.bpi.contract.compatibility;

import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PointCatalogSnapshotV1Test {

    @Test
    public void roundTripsImmutableSourceRevisionAndReadinessEvidence() throws Exception {
        PointCatalogPointV1 point = PointCatalogPointV1.newBuilder()
            .setLocalityGroup("LINE-S07.feed")
            .setProductId("flow-meter")
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
            .setSourceSequenceOrigin(SequenceOrigin.GATEWAY)
            .setSourceSequenceBindingFingerprint(
                "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            )
            .build();
        PointCatalogSnapshotV1 snapshot = PointCatalogSnapshotV1.newBuilder()
            .setEventId("catalog-event-01")
            .setSource("JETLINKS")
            .setSourceInstance("jetlinks-pilot")
            .setSourceRevision("sha256:catalog-revision")
            .setTenantId("tenant-01")
            .setPlantId("plant-01")
            .setLineId("line-01")
            .setObservedAtMs(1_720_000_000_000L)
            .setReason("Automatic JetLinks point catalog synchronization")
            .addPoints(point)
            .build();

        PointCatalogSnapshotV1 decoded = PointCatalogSnapshotV1.parseFrom(snapshot.toByteArray());

        assertEquals(snapshot, decoded);
        assertEquals("sha256:catalog-revision", decoded.getSourceRevision());
        assertEquals(PointDeviceStateV1.POINT_DEVICE_ACTIVE, decoded.getPoints(0).getDeviceState());
        assertTrue(decoded.getPoints(0).getSourceSequenceEnabled());
        assertTrue(decoded.getPoints(0).getSourceSequenceRequired());
        assertEquals(SequenceOrigin.GATEWAY, decoded.getPoints(0).getSourceSequenceOrigin());
    }

    @Test
    public void roundTripsDeviceLevelSourceSequenceEvidence() throws Exception {
        SourceSequenceEvidenceV1 evidence = SourceSequenceEvidenceV1.newBuilder()
            .setEventId("source-sequence-evidence-01")
            .setSource("JETLINKS")
            .setSourceInstance("jetlinks-pilot")
            .setTenantId("tenant-01")
            .setPlantId("plant-01")
            .setLineId("line-01")
            .setProductId("flow-meter")
            .setDeviceId("meter-01")
            .setBindingFingerprint(
                "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            )
            .setStatus(SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED)
            .setSequenceOrigin(SequenceOrigin.GATEWAY)
            .setSourceEpoch(7L)
            .setFirstSequence(42L)
            .setLastSequence(43L)
            .setObservationCount(2)
            .setFirstObservedAtMs(1_720_000_000_000L)
            .setLastObservedAtMs(1_720_000_001_000L)
            .setValidUntilMs(1_720_001_801_000L)
            .setObservedAtMs(1_720_000_001_500L)
            .setReason("Runtime source sequence qualification")
            .build();

        SourceSequenceEvidenceV1 decoded = SourceSequenceEvidenceV1.parseFrom(evidence.toByteArray());

        assertEquals(evidence, decoded);
        assertEquals(SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED, decoded.getStatus());
        assertEquals(2, decoded.getObservationCount());
        assertEquals(43L, decoded.getLastSequence());
    }
}
