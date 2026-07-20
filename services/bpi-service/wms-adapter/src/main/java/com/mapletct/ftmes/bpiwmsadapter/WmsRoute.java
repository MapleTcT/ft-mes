package com.mapletct.ftmes.bpiwmsadapter;

public record WmsRoute(
        String tenantId,
        String plantId,
        String lineId,
        String warehouseCode,
        String locationCode,
        String companyCode,
        String baseUnit) {

    static WmsRoute parse(String definition) {
        String[] values = definition == null ? new String[0] : definition.split("\\|", -1);
        if (values.length != 7) {
            throw new IllegalArgumentException(
                    "BPI WMS route must contain tenant|plant|line|warehouse|location|company|baseUnit.");
        }
        for (int index = 0; index < values.length; index++) {
            values[index] = values[index].trim();
            if (values[index].isEmpty()) {
                throw new IllegalArgumentException("BPI WMS route segment " + index + " cannot be blank.");
            }
        }
        return new WmsRoute(
                values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }

    boolean matches(String tenantId, String plantId, String lineId) {
        return this.tenantId.equals(tenantId)
                && this.plantId.equals(plantId)
                && this.lineId.equals(lineId);
    }
}
