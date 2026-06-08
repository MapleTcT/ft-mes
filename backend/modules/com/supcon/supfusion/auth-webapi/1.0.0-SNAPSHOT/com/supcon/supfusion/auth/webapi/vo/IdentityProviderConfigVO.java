package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * 身份提供者配置参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("身份提供者配置参数模型")
public class IdentityProviderConfigVO {

    @ApiModelProperty(value = "获取token地址", example = "https://auth.supcon.com/auth/access_token")
    private String tokenUrl;

    @ApiModelProperty(value = "获取用户信息地址", example = "https://auth.supcon.com/auth/user_info")
    private String profileUrl;

    @NotEmpty(message = "客户端id不能为空")
    @ApiModelProperty(value = "客户端id", example = "app001")
    private String clientId;

    @ApiModelProperty(value = "客户端密钥", example = "123456")
    private String clientSecret;

    @ApiModelProperty(value = "回调地址", example = "https://jbsh.com/auth/callback")
    private String redirectUri;

    @ApiModelProperty(value = "授权作用域", example = "snsapi_login")
    private String scope;
}
