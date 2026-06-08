package com.supcon.supfusion.license.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class LicenseInfoDTO extends DTO {

    public LicenseInfoDTO(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    @NotEmpty(message = "moduleCode is not empty")
    private String moduleCode;
    @NotEmpty(message = "licenseKey is not empty")
    private String licenseKey;
    private Integer value;
    //    private String linuxLicenseKey;
    private String time;
    private String applicationName;
    private String applicationType;
    private String description;
}
