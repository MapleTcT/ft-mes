package com.mapletct.ftmes.materialwms.domain;

public enum DocumentType {
    COMPLETION_INBOUND("CIN"),
    PRODUCTION_ISSUE("PIS");

    private final String numberPrefix;

    DocumentType(String numberPrefix) {
        this.numberPrefix = numberPrefix;
    }

    public String getNumberPrefix() {
        return numberPrefix;
    }
}
