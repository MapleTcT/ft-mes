package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record ReviewView(String actor, String reason, Instant at) {
}
