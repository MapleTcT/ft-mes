package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;

import java.util.HashSet;
import java.util.Set;

final class PointCatalogRuntimeValidator {

    private static final int MAX_POINTS = 10_000;

    private PointCatalogRuntimeValidator() {
    }

    static PointCatalogSnapshotV1 validate(PointCatalogSnapshotV1 snapshot) {
        require(snapshot.getEventId(), "event_id");
        require(snapshot.getSource(), "source");
        require(snapshot.getSourceInstance(), "source_instance");
        require(snapshot.getSourceRevision(), "source_revision");
        require(snapshot.getTenantId(), "tenant_id");
        require(snapshot.getPlantId(), "plant_id");
        require(snapshot.getLineId(), "line_id");
        if (snapshot.getObservedAtMs() <= 0) {
            throw new IllegalArgumentException("observed_at_ms must be positive");
        }
        if (snapshot.getPointsCount() > MAX_POINTS) {
            throw new IllegalArgumentException("point catalog exceeds 10000 points");
        }
        Set<String> identities = new HashSet<>();
        for (PointCatalogPointV1 point : snapshot.getPointsList()) {
            validate(point);
            String identity = pointKey(snapshot, point);
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("duplicate point catalog identity: " + identity);
            }
        }
        return snapshot;
    }

    static String scopeKey(PointCatalogSnapshotV1 snapshot) {
        return String.join("|", snapshot.getTenantId(), snapshot.getPlantId(), snapshot.getLineId());
    }

    static String pointKey(PointCatalogSnapshotV1 snapshot, PointCatalogPointV1 point) {
        return pointKey(
                snapshot.getTenantId(), snapshot.getPlantId(), snapshot.getLineId(),
                point.getProductId(), point.getDeviceId(), point.getPropertyId());
    }

    static String pointKey(
            String tenantId,
            String plantId,
            String lineId,
            String productId,
            String deviceId,
            String propertyId) {
        return String.join("|", tenantId, plantId, lineId, productId, deviceId, propertyId);
    }

    private static void validate(PointCatalogPointV1 point) {
        require(point.getProductId(), "point.product_id");
        require(point.getDeviceId(), "point.device_id");
        require(point.getPropertyId(), "point.property_id");
        if (point.getDeviceState() == PointDeviceStateV1.POINT_DEVICE_STATE_UNSPECIFIED
                || point.getDeviceState() == PointDeviceStateV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("point.device_state must be explicit");
        }
        if (point.getCalibrationStatus() == PointCalibrationStatusV1.POINT_CALIBRATION_STATUS_UNSPECIFIED
                || point.getCalibrationStatus() == PointCalibrationStatusV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("point.calibration_status must be explicit");
        }
        if (point.getCalibrationStatus() == PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED) {
            require(point.getCalibrationVersion(), "point.calibration_version");
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(field + " must be nonblank and cannot contain '|'");
        }
    }
}
