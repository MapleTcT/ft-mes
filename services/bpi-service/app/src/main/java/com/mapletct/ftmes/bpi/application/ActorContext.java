package com.mapletct.ftmes.bpi.application;

import java.util.Set;

public record ActorContext(
        String tenantId,
        String userId,
        Set<String> roles,
        Set<String> plantIds,
        Set<String> lineIds) {

    public boolean canAccess(String plantId, String lineId) {
        return (plantIds.contains("*") || plantIds.contains(plantId))
                && (lineIds.contains("*") || lineIds.contains(lineId));
    }
}
