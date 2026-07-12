package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record BatchStateEvent(
        long revision,
        String action,
        Instant at,
        String actor,
        String reason) {
}
