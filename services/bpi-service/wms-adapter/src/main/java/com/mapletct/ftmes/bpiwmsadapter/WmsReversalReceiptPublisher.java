package com.mapletct.ftmes.bpiwmsadapter;

import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalCommandV1;

public interface WmsReversalReceiptPublisher {

    void accepted(
            WmsCompletionInboundReversalCommandV1 command,
            MaterialWmsReversalDocument document,
            String detail);

    void rejected(
            WmsCompletionInboundReversalCommandV1 command,
            String errorCode,
            String detail);
}
