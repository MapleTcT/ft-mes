/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.feign.dto;

import java.io.Serializable;

public class LicenseInfoDTO
implements Serializable {
    private String moduleCode;
    private String licenseKey;
    private Integer value;
    private String moduleType;
    private String description;
    private String time;

    public String getModuleCode() {
        return this.moduleCode;
    }

    public String getLicenseKey() {
        return this.licenseKey;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getModuleType() {
        return this.moduleType;
    }

    public String getDescription() {
        return this.description;
    }

    public String getTime() {
        return this.time;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LicenseInfoDTO)) {
            return false;
        }
        LicenseInfoDTO other = (LicenseInfoDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$moduleCode = this.getModuleCode();
        String other$moduleCode = other.getModuleCode();
        if (this$moduleCode == null ? other$moduleCode != null : !this$moduleCode.equals(other$moduleCode)) {
            return false;
        }
        String this$licenseKey = this.getLicenseKey();
        String other$licenseKey = other.getLicenseKey();
        if (this$licenseKey == null ? other$licenseKey != null : !this$licenseKey.equals(other$licenseKey)) {
            return false;
        }
        Integer this$value = this.getValue();
        Integer other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        String this$moduleType = this.getModuleType();
        String other$moduleType = other.getModuleType();
        if (this$moduleType == null ? other$moduleType != null : !this$moduleType.equals(other$moduleType)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$time = this.getTime();
        String other$time = other.getTime();
        return !(this$time == null ? other$time != null : !this$time.equals(other$time));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LicenseInfoDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $moduleCode = this.getModuleCode();
        result = result * 59 + ($moduleCode == null ? 43 : $moduleCode.hashCode());
        String $licenseKey = this.getLicenseKey();
        result = result * 59 + ($licenseKey == null ? 43 : $licenseKey.hashCode());
        Integer $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        String $moduleType = this.getModuleType();
        result = result * 59 + ($moduleType == null ? 43 : $moduleType.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        return result;
    }

    public String toString() {
        return "LicenseInfoDTO(moduleCode=" + this.getModuleCode() + ", licenseKey=" + this.getLicenseKey() + ", value=" + this.getValue() + ", moduleType=" + this.getModuleType() + ", description=" + this.getDescription() + ", time=" + this.getTime() + ")";
    }
}

