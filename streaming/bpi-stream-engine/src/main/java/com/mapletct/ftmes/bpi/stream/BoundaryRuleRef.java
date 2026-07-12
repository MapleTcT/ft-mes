package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record BoundaryRuleRef(String ruleCode, String ruleVersion) {

    public BoundaryRuleRef {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(ruleVersion, "ruleVersion");
        if (ruleCode.isBlank() || ruleVersion.isBlank()
                || ruleCode.indexOf('|') >= 0 || ruleVersion.indexOf('|') >= 0) {
            throw new IllegalArgumentException("rule identifiers must be nonblank and cannot contain '|'");
        }
    }

    public String key() {
        return ruleCode + "|" + ruleVersion;
    }
}
