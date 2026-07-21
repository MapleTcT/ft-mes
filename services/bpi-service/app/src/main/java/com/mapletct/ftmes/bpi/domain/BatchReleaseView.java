package com.mapletct.ftmes.bpi.domain;

public record BatchReleaseView(
        BatchInstance batch,
        QualityGateView qualityGate,
        WmsInboundView wmsInbound,
        WmsInboundReversalTaskView wmsInboundReversal) {
}
