package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * ip黑白名单意义参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("ip黑白名单操作提示参数模型")
public class IpBlackWhiteOperateTipVO {

    @NotEmpty(message = "IP不能为空")
    @ApiModelProperty(value = "IP，支持通配符", example = "xxx.xxx.xx.xxx", required = true)
    private String ip;

    @NotNull(message = "管控模式不能为空")
    @ApiModelProperty(value = "管控模式 0:黑名单 1:白名单", example = "0", required = true)
    private Integer controlType;

    @NotNull(message = "操作类型不能为空")
    @ApiModelProperty(value = "操作类型，0：新增 1：删除", example = "0", required = true)
    private Integer operateType;

}
