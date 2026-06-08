package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * ip黑白名单参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("ip黑白名单参数模型")
public class IpBlackWhiteVO {

    @NotEmpty(message = "访问IP不能为空")
    @ApiModelProperty(value = "访问IP，支持通配符", example = "xxx.xxx.xxx.xxx", required = true)
    private String ip;

    @NotNull(message = "管控模式不能为空")
    @ApiModelProperty(value = "管控模式 0:黑名单 1:白名单", example = "0", required = true)
    private Integer controlType;

    @ApiModelProperty(value = "是否添加当前登录ip", example = "false", required = false)
    private Boolean addCurrentIp = false;
}
