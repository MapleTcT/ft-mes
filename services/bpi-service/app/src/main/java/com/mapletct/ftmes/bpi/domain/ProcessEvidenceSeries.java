package com.mapletct.ftmes.bpi.domain;

import java.util.List;

public record ProcessEvidenceSeries(
        String propertyId,
        String deviceId,
        String unit,
        int sourceCount,
        boolean truncated,
        Double minimum,
        Double maximum,
        Double average,
        List<ProcessEvidenceSample> samples) {
}
