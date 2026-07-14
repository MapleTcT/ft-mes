package com.mapletct.ftmes.bpi.contract.compatibility;

import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
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
    }
}
