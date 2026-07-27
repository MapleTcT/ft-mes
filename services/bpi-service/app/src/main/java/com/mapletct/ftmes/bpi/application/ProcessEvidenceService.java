package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.ProcessEvidenceSample;
import com.mapletct.ftmes.bpi.domain.ProcessEvidenceSeries;
import com.mapletct.ftmes.bpi.domain.ProcessEvidenceView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.ProcessEvidencePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.ProcessEvidencePostgresRepository.EvidenceRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ProcessEvidenceService {
    static final int MAX_PROPERTY_COUNT = 16;
    static final int MAX_SOURCE_ROWS = 5_000;
    static final int MAX_SAMPLES_PER_SERIES = 240;
    private static final Duration MAX_WINDOW = Duration.ofHours(24);
    private static final Pattern SCOPE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final ProcessEvidencePostgresRepository repository;

    public ProcessEvidenceService(ProcessEvidencePostgresRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProcessEvidenceView get(
            ActorContext actor,
            String plantId,
            String lineId,
            String orderId,
            Instant from,
            Instant to,
            List<String> propertyIds) {
        String normalizedPlant = requiredId(plantId, "plantId");
        String normalizedLine = requiredId(lineId, "lineId");
        String normalizedOrder = requiredId(orderId, "orderId");
        if (!actor.canAccess(normalizedPlant, normalizedLine)) {
            throw new BpiForbiddenException("Token scope does not allow this BPI line.");
        }
        if (from == null || to == null) {
            throw new BpiValidationException("from and to are required.");
        }
        Duration window = Duration.between(from, to);
        if (window.isNegative() || window.isZero()) {
            throw new BpiValidationException("to must be later than from.");
        }
        if (window.compareTo(MAX_WINDOW) > 0) {
            throw new BpiValidationException("Process evidence window cannot exceed 24 hours.");
        }

        List<String> normalizedProperties = normalizeProperties(propertyIds);
        List<EvidenceRow> fetched = repository.list(
                actor.tenantId(),
                normalizedPlant,
                normalizedLine,
                from,
                to,
                normalizedProperties,
                MAX_SOURCE_ROWS + 1);
        boolean queryTruncated = fetched.size() > MAX_SOURCE_ROWS;
        List<EvidenceRow> rows = queryTruncated
                ? fetched.subList(0, MAX_SOURCE_ROWS)
                : fetched;
        Map<SeriesKey, List<EvidenceRow>> grouped = new LinkedHashMap<>();
        for (EvidenceRow row : rows) {
            grouped.computeIfAbsent(
                    new SeriesKey(row.propertyId(), row.deviceId(), row.unit()),
                    ignored -> new ArrayList<>()).add(row);
        }

        List<ProcessEvidenceSeries> series = grouped.entrySet().stream()
                .map(entry -> series(entry.getKey(), entry.getValue(), queryTruncated))
                .toList();
        return new ProcessEvidenceView(
                actor.tenantId(),
                normalizedPlant,
                normalizedLine,
                normalizedOrder,
                from,
                to,
                true,
                series);
    }

    private ProcessEvidenceSeries series(
            SeriesKey key,
            List<EvidenceRow> rows,
            boolean queryTruncated) {
        List<Double> numericValues = rows.stream()
                .map(EvidenceRow::numericValue)
                .filter(value -> value != null)
                .toList();
        Double minimum = numericValues.stream().min(Double::compareTo).orElse(null);
        Double maximum = numericValues.stream().max(Double::compareTo).orElse(null);
        Double average = numericValues.isEmpty()
                ? null
                : numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        List<EvidenceRow> sampled = downsample(rows, MAX_SAMPLES_PER_SERIES);
        return new ProcessEvidenceSeries(
                key.propertyId(),
                key.deviceId(),
                key.unit(),
                rows.size(),
                queryTruncated || sampled.size() < rows.size(),
                minimum,
                maximum,
                average,
                sampled.stream().map(row -> new ProcessEvidenceSample(
                        row.sampleTime(),
                        row.numericValue(),
                        row.stringValue(),
                        row.booleanValue(),
                        row.qualityCode())).toList());
    }

    private List<EvidenceRow> downsample(List<EvidenceRow> rows, int maximum) {
        if (rows.size() <= maximum) {
            return List.copyOf(rows);
        }
        List<EvidenceRow> sampled = new ArrayList<>(maximum);
        int lastIndex = rows.size() - 1;
        for (int index = 0; index < maximum; index++) {
            int sourceIndex = (int) Math.floor((double) index * lastIndex / (maximum - 1));
            sampled.add(rows.get(sourceIndex));
        }
        return sampled;
    }

    private List<String> normalizeProperties(List<String> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return List.of();
        }
        if (propertyIds.size() > MAX_PROPERTY_COUNT) {
            throw new BpiValidationException("No more than 16 property filters are allowed.");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String propertyId : propertyIds) {
            normalized.add(requiredId(propertyId, "property"));
        }
        return List.copyOf(normalized);
    }

    private String requiredId(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (!SCOPE_ID.matcher(normalized).matches()) {
            throw new BpiValidationException(name + " must match [A-Za-z0-9._:-]{1,128}.");
        }
        return normalized;
    }

    private record SeriesKey(String propertyId, String deviceId, String unit) {
    }
}
