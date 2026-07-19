package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.domain.PointCalibrationView;

import java.time.Instant;
import java.util.List;

public record PointCalibrationPage(
        List<PointCalibrationView> items,
        Instant snapshotAt,
        String nextCursor) {

    public PointCalibrationPage {
        items = List.copyOf(items);
    }
}
