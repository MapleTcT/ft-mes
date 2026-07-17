package com.mapletct.ftmes.materialwms.domain;

import java.util.Locale;

public enum QualityStatus {
    PENDING,
    QUALIFIED,
    PARTIAL,
    UNQUALIFIED;

    public static QualityStatus fromLegacy(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("unqualified") || normalized.contains("不合格")) {
            return UNQUALIFIED;
        }
        if (normalized.contains("qualified") || normalized.contains("合格")) {
            return QUALIFIED;
        }
        if (normalized.contains("pending") || normalized.contains("待检") || normalized.contains("未检")) {
            return PENDING;
        }
        throw new MaterialWmsBusinessException(400, "不支持的质检结果: " + value);
    }
}
