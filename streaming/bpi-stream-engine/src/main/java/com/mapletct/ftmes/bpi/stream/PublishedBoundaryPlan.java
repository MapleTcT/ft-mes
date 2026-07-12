package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;

import java.util.Map;
import java.util.Objects;

public record PublishedBoundaryPlan(
        BoundaryRulePublicationV1 publication,
        BoundaryRuleDefinition rule,
        Map<String, BoundarySignalBindingV1> bindings) {

    public PublishedBoundaryPlan {
        Objects.requireNonNull(publication, "publication");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(bindings, "bindings");
        bindings = Map.copyOf(bindings);
    }

    public BoundaryRuleRef ruleRef() {
        return new BoundaryRuleRef(rule.ruleCode(), rule.ruleVersion());
    }

    public BoundaryRuleUpdate ruleUpdate() {
        return publication.getActive()
                ? BoundaryRuleUpdate.upsert(rule)
                : BoundaryRuleUpdate.delete(rule.ruleCode(), rule.ruleVersion());
    }

    static String bindingKey(String deviceId, String propertyId) {
        return deviceId + "|" + propertyId;
    }
}
