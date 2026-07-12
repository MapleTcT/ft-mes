package com.mapletct.ftmes.bpi.interfaces.rest;

import java.time.Instant;

public record ResponseMeta(String traceId, Instant generatedAt, Instant snapshotAt, String nextCursor) {
}
