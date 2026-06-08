package com.supcon.supfusion.rbac.common.enumerate;

public enum BAPLicenseKey {
    /**
     * bap平台
     */
    BAP("supPlant-Server-S0C", "supPlant-Server-S0C",
            "supPlant应用服务器软件",
            "EdrvXM2VSorwfKb4iDrzMPMOQapgN5//3HOkrltncTPXKqYwEtTtcqxWPTxOx3LDnoI+iw=="),
    /**
     * 实体配置
     */
    EC_MODULE("supPlant-Dev", "supPlant-Dev",
            "supPlant应用模块开发平台软件",
            "EdrvXM2VSorwfKb4iDrzMMRgDzfLimq73HOkrltncTOK1xXIAa+5nQJum1DguTH2XIxtGQ==");
    private String moduleCode;
    private String applicationType;
    private String applicationName;
    private String licenseKey;

    BAPLicenseKey(String moduleCode, String applicationType, String applicationName, String licenseKey) {
        this.moduleCode = moduleCode;
        this.applicationType = applicationType;
        this.applicationName = applicationName;
        this.licenseKey = licenseKey;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
}
