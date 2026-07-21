package com.mapletct.ftmes.qcsoutbox;

public interface BpiBatchResolver {
    ResolvedBpiBatch resolve(QcsQualityGateOutboxRecord record);
}
