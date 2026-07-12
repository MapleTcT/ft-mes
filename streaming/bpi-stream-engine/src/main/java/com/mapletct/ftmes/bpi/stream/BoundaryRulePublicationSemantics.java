package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;

final class BoundaryRulePublicationSemantics {

    private BoundaryRulePublicationSemantics() {
    }

    static boolean equivalent(
            BoundaryRulePublicationV1 left,
            BoundaryRulePublicationV1 right) {
        return semantic(left).equals(semantic(right));
    }

    static String key(BoundaryRulePublicationV1 publication) {
        return new BoundaryRuleRef(
                publication.getTenantId(),
                publication.getPlantId(),
                publication.getLineId(),
                publication.getRuleCode(),
                publication.getRuleVersion()).key();
    }

    private static BoundaryRulePublicationV1 semantic(BoundaryRulePublicationV1 publication) {
        return publication.toBuilder()
                .clearEventId()
                .clearActive()
                .clearPublishedAtMs()
                .clearHeaders()
                .build();
    }
}
