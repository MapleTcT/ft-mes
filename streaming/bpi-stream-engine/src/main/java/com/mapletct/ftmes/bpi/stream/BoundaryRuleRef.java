package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record BoundaryRuleRef(
        String tenantId,
        String plantId,
        String lineId,
        String ruleCode,
        String ruleVersion) {

    public BoundaryRuleRef(String ruleCode, String ruleVersion) {
        this("", "", "", ruleCode, ruleVersion);
    }

    public BoundaryRuleRef {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(plantId, "plantId");
        Objects.requireNonNull(lineId, "lineId");
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(ruleVersion, "ruleVersion");
        boolean unscoped = tenantId.isBlank() && plantId.isBlank() && lineId.isBlank();
        boolean scoped = !tenantId.isBlank() && !plantId.isBlank() && !lineId.isBlank();
        if ((!unscoped && !scoped)
                || ruleCode.isBlank()
                || ruleVersion.isBlank()
                || containsSeparator(tenantId)
                || containsSeparator(plantId)
                || containsSeparator(lineId)
                || containsSeparator(ruleCode)
                || containsSeparator(ruleVersion)) {
            throw new IllegalArgumentException(
                    "rule scope and identifiers must be either fully scoped or unscoped and cannot contain '|'");
        }
    }

    public String key() {
        if (tenantId.isBlank()) {
            return ruleCode + "|" + ruleVersion;
        }
        return String.join("|", tenantId, plantId, lineId, ruleCode, ruleVersion);
    }

    public boolean scoped() {
        return !tenantId.isBlank();
    }

    private static boolean containsSeparator(String value) {
        return value.indexOf('|') >= 0;
    }
}
