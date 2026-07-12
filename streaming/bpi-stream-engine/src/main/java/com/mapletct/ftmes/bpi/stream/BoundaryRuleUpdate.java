package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;

import java.util.Objects;

public record BoundaryRuleUpdate(
        Operation operation,
        BoundaryRuleRef ruleRef,
        BoundaryRuleDefinition rule) {

    public BoundaryRuleUpdate {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(ruleRef, "ruleRef");
        if (operation == Operation.UPSERT) {
            Objects.requireNonNull(rule, "rule");
            if (!ruleRef.ruleCode().equals(rule.ruleCode())
                    || !ruleRef.ruleVersion().equals(rule.ruleVersion())) {
                throw new IllegalArgumentException("rule update identity does not match its definition");
            }
        } else if (rule != null) {
            throw new IllegalArgumentException("DELETE update cannot carry a rule definition");
        }
    }

    public static BoundaryRuleUpdate upsert(BoundaryRuleDefinition rule) {
        return new BoundaryRuleUpdate(
                Operation.UPSERT,
                new BoundaryRuleRef(rule.ruleCode(), rule.ruleVersion()),
                rule);
    }

    public static BoundaryRuleUpdate delete(String ruleCode, String ruleVersion) {
        return new BoundaryRuleUpdate(
                Operation.DELETE,
                new BoundaryRuleRef(ruleCode, ruleVersion),
                null);
    }

    public enum Operation {
        UPSERT,
        DELETE
    }
}
