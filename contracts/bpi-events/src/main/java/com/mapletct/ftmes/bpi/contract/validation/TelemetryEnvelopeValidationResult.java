package com.mapletct.ftmes.bpi.contract.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TelemetryEnvelopeValidationResult {

    private final List<ContractViolation> envelopeViolations;
    private final List<Integer> acceptedPointIndexes;
    private final List<PointRejection> pointRejections;

    TelemetryEnvelopeValidationResult(
        List<ContractViolation> envelopeViolations,
        List<Integer> acceptedPointIndexes,
        List<PointRejection> pointRejections
    ) {
        this.envelopeViolations = immutableCopy(envelopeViolations);
        this.acceptedPointIndexes = immutableCopy(acceptedPointIndexes);
        this.pointRejections = immutableCopy(pointRejections);
    }

    public boolean isEnvelopeAccepted() {
        return envelopeViolations.isEmpty();
    }

    public List<ContractViolation> getEnvelopeViolations() {
        return envelopeViolations;
    }

    public List<Integer> getAcceptedPointIndexes() {
        return acceptedPointIndexes;
    }

    public List<PointRejection> getPointRejections() {
        return pointRejections;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
