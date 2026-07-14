package com.mapletct.ftmes.bpi.domain;

import java.util.List;

public record PointCatalogView(
        PointCatalogSnapshotView snapshot,
        List<PointCatalogPointView> points) {
}
