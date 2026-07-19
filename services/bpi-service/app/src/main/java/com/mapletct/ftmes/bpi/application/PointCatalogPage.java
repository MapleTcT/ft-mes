package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.domain.PointCatalogView;

import java.time.Instant;

public record PointCatalogPage(
        PointCatalogView catalog,
        Instant snapshotAt,
        String nextCursor) {
}
