package com.mapletct.ftmes.bpi.domain;

import java.util.List;

public record DataQualityIncidentDetail(
        DataQualityIncidentView incident,
        List<DataQualityEventView> events,
        List<DataQualityLifecycleView> lifecycle,
        List<String> recommendedActions) {

    public DataQualityIncidentDetail {
        events = List.copyOf(events);
        lifecycle = List.copyOf(lifecycle);
        recommendedActions = List.copyOf(recommendedActions);
    }
}
