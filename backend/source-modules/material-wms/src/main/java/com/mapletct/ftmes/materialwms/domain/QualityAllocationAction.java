package com.mapletct.ftmes.materialwms.domain;

import java.util.Locale;

public enum QualityAllocationAction {
    APPLY,
    REVERSE;

    public static QualityAllocationAction from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new MaterialWmsBusinessException(400, "action 不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new MaterialWmsBusinessException(400, "action 必须是 APPLY 或 REVERSE");
        }
    }
}
