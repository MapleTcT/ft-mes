package com.mapletct.ftmes.bpi.contract.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PointRejection {

    private final int pointIndex;
    private final List<ContractViolation> violations;

    public PointRejection(int pointIndex, List<ContractViolation> violations) {
        this.pointIndex = pointIndex;
        this.violations = Collections.unmodifiableList(new ArrayList<ContractViolation>(violations));
    }

    public int getPointIndex() {
        return pointIndex;
    }

    public List<ContractViolation> getViolations() {
        return violations;
    }
}
