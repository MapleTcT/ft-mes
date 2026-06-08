package com.supcon.supfusion.license.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseInfoBO implements Serializable {
    private String moduleCode;
    private String licenseKey;
    private Integer value;
//    private String linuxLicenseKey;
    private String time;
    private String applicationName;
    private String applicationType;
    private String description;
}
