package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record GoldenBoundary(Instant boundaryTime, int toleranceSeconds) {
}
