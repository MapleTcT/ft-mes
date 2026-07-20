package com.mapletct.ftmes.bpiwmsadapter;

import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;

public interface WmsReceiptPublisher {

    void accepted(WmsCompletionInboundCommandV1 command, MaterialWmsDocument document, String detail);

    void rejected(WmsCompletionInboundCommandV1 command, String errorCode, String detail);
}
