package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IntegrationBatchResolution(
        UUID id,
        String batchNo,
        String tenantId,
        String plantId,
        String lineId,
        String stageCode,
        String orderId,
        String materialCode,
        BatchState state,
        long revision,
        boolean shadow,
        Instant startTime,
        Instant endTime,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal dryMatter,
        String qualityGate,
        String wmsStatus,
        String ruleVersion,
        String topologyVersion,
        String currentQualityGateId,
        Long currentQualityGateRevision,
        String currentQualityGateSourceEventId) {

    public static IntegrationBatchResolution from(BatchReleaseView release) {
        BatchInstance batch = release.batch();
        QualityGateView gate = release.qualityGate();
        return new IntegrationBatchResolution(
                batch.id(),
                batch.batchNo(),
                batch.tenantId(),
                batch.plantId(),
                batch.lineId(),
                batch.stageCode(),
                batch.orderId(),
                batch.materialCode(),
                batch.state(),
                batch.revision(),
                batch.shadow(),
                batch.startTime(),
                batch.endTime(),
                batch.quantity(),
                batch.quantityUnit(),
                batch.dryMatter(),
                batch.qualityGate(),
                batch.wmsStatus(),
                batch.ruleVersion(),
                batch.topologyVersion(),
                gate == null ? null : gate.externalGateId(),
                gate == null ? null : gate.externalRevision(),
                gate == null ? null : gate.sourceEventId());
    }
}
