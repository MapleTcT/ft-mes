package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * 身份提供者参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("身份提供者参数模型")
public class IdentityProviderVO {

    @NotEmpty(message = "身份提供商名称不能为空")
    @ApiModelProperty(value = "身份提供商名称", example = "京博石化", required = true)
    private String name;

    @NotEmpty(message = "协议类型不能为空")
    @ApiModelProperty(value = "协议类型 系统编码", example = "OAuth2", required = true)
    private String protocolType;

    @NotEmpty(message = "授权地址不能为空")
    @ApiModelProperty(value = "授权地址", example = "https://auth.supcon.com/auth/authorization", required = true)
    private String authUrl;

    @ApiModelProperty(value = "描述", example = "认证提供者是京博石化", required = false)
    private String description;
}
