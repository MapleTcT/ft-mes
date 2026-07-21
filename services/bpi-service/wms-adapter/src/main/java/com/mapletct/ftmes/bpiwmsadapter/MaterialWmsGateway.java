package com.mapletct.ftmes.bpiwmsadapter;

import java.util.Optional;

public interface MaterialWmsGateway {

    Optional<MaterialWmsDocument> findByIdempotency(String tenantId, String idempotencyKey);

    void createCompletionInbound(MaterialWmsCreateRequest request);

    Optional<MaterialWmsReversalDocument> findReversalByIdempotency(
            String tenantId, String idempotencyKey);

    void createCompletionInboundReversal(MaterialWmsReversalRequest request);
}
