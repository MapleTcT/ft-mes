package com.supcon.supfusion.license.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "授权信息类")
public class LicenseInfoVO {
    @ApiModelProperty(value = "模块编码")
    private String moduleCode;
    @ApiModelProperty(value = "软件狗key")
    private String licenseKey;
    @ApiModelProperty(value = "授权值")
    private Integer value;
    //    private String linuxLicenseKey;
    @ApiModelProperty("APP启动时的时间")
    private String time;
    @ApiModelProperty("应用模块名称(授权中文)")
    private String applicationName;
    @ApiModelProperty("应用模块型号(授权英文)")
    private String applicationType;
    @ApiModelProperty("授权情况")
    private String description;
}
