package com.supcon.supfusion.auditlog.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 模型启用/不启用审计日志
 * @author caokele
 */
@ApiModel
@Data
public class ModelAuditLogReqVO {

    @ApiModelProperty(value = "是否启用审计日志", required = true)
    private Boolean enable;

    @ApiModelProperty(value = "模型编码", required = true)
    private Map<String, String> modelCodeMap;
}
