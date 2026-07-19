package com.mapletct.ftmes.bpi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShadowRunReadiness(
        boolean rulePublished,
        boolean ruleActive,
        boolean publicationConfirmed,
        boolean applicationApplied,
        boolean runtimeReady,
        boolean topologyPublished,
        boolean topologySnapshotPinned,
        boolean pointCatalogCurrent,
        boolean pointCatalogReady) {

    @JsonProperty("ready")
    public boolean ready() {
        return rulePublished
                && ruleActive
                && publicationConfirmed
                && applicationApplied
                && runtimeReady
                && topologyPublished
                && topologySnapshotPinned
                && pointCatalogCurrent
                && pointCatalogReady;
    }
}
