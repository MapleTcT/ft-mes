package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.domain.DataQualityIncidentView;

import java.time.Instant;
import java.util.List;

public record DataQualityIncidentPage(
        List<DataQualityIncidentView> items,
        Instant snapshotAt,
        String nextCursor) {

    public DataQualityIncidentPage {
        items = List.copyOf(items);
    }
}
