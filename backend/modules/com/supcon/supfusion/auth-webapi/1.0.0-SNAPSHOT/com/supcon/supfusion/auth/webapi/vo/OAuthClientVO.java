package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 认证客户端参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("认证客户端参数模型")
public class OAuthClientVO {

    @NotNull(message = "认证中心ID不能为空")
    @ApiModelProperty(value = "认证中心id", name = "id", example = "680038889177123", required = true)
    private Long authCenterId;

    @NotEmpty(message = "客户端名称不能为空")
    @ApiModelProperty(value = "客户端名称", example = "京博石化", required = true)
    private String name;

    @NotEmpty(message = "授权模式不能为空")
    @ApiModelProperty(value = "授权模式 系统编码", example = "authorizationCode", required = true)
    private String grantType;

    @NotEmpty(message = "clientId不能为空")
    @ApiModelProperty(value = "clientId", example = "app001", required = true)
    private String clientId;

    @ApiModelProperty(value = "客户端密钥", example = "123456", required = false)
    private String clientSecret;

    @ApiModelProperty(value = "回调地址", example = "https://jbsh.com/auth/callback", required = false)
    private String redirectUri;

    @ApiModelProperty(value = "授权作用域", example = "snsapi_login", required = false)
    private String scope;

    @ApiModelProperty(value = "有效期(秒)", example = "7200", required = false)
    private Integer expiresIn;

    @ApiModelProperty(value = "认证方式 系统编码", example = "np", required = false)
    private String authMethod;

    @ApiModelProperty(value = "描述", example = "京博石化的客户端", required = false)
    private String description;
}
