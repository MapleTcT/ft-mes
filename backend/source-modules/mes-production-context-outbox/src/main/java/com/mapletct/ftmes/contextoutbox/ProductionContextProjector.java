package com.mapletct.ftmes.contextoutbox;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.validation.ContractViolation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductionContextProjector {

    public ProductionContextEventV1 project(ProductionContextOutboxRecord record) {
        ProductionContextEventV1.Builder builder = ProductionContextEventV1.newBuilder()
            .setEventId(value(record.getEventId()))
            .setTenantId(value(record.getTenantId()))
            .setPlantId(value(record.getPlantId()))
            .setLineId(value(record.getLineId()))
            .setOrderId(value(record.getOrderId()))
            .setTaskId(value(record.getTaskId()))
            .setMaterialCode(value(record.getMaterialCode()))
            .setRecipeVersion(value(record.getRecipeVersion()))
            .setBatchId(value(record.getBatchId()))
            .setEffectiveFromMs(record.getEffectiveFromMs())
            .setEffectiveToMs(record.getEffectiveToMs() == null ? 0L : record.getEffectiveToMs())
            .setContextRevision(record.getContextRevision())
            .setActive(record.isActive())
            .putAttributes("source", "WOM")
            .putAttributes("outbox_id", Long.toString(record.getId()));

        put(builder, "wom_cid", record.getWomCid());
        put(builder, "wom_line_id", record.getWomLineId());
        put(builder, "source_state", record.getSourceState());

        ProductionContextEventV1 event = builder.build();
        List<ContractViolation> violations = BpiContractValidator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("invalid production context: " + violations);
        }
        return event;
    }

    private static void put(ProductionContextEventV1.Builder builder, String key, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            builder.putAttributes(key, String.valueOf(value));
        }
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
