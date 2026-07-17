package com.mapletct.ftmes.womprint.domain;

import java.util.Collections;
import java.util.List;

public final class GenerationResult {

    private final String requestId;
    private final List<String> details;
    private final boolean idempotent;

    public GenerationResult(String requestId, List<String> details, boolean idempotent) {
        this.requestId = requestId;
        this.details = Collections.unmodifiableList(details);
        this.idempotent = idempotent;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<String> getDetails() {
        return details;
    }

    public boolean isIdempotent() {
        return idempotent;
    }
}
