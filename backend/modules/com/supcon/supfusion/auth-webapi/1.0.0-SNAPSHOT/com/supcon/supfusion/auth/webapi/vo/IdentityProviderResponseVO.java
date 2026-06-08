package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 身份提供者响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("身份提供者响应模型")
public class IdentityProviderResponseVO {
    @ApiModelProperty(value = "主键id", name = "id", example = "580038889177088")
    private Long id;

    @ApiModelProperty(value = "身份提供商名称", example = "京博石化")
    private String name;

    @ApiModelProperty(value = "协议类型 系统编码", example = "OAuth2")
    private SystemCodeResultDTO protocolType;

    @ApiModelProperty(value = "授权地址", example = "https://auth.supcon.com/auth/authorization")
    private String authUrl;

    @ApiModelProperty(value = "获取token地址", example = "https://auth.supcon.com/auth/access_token")
    private String tokenUrl;

    @ApiModelProperty(value = "获取用户信息地址", example = "https://auth.supcon.com/auth/user_info")
    private String profileUrl;

    @ApiModelProperty(value = "客户端id", example = "app001")
    private String clientId;

    @ApiModelProperty(value = "客户端密钥", example = "123456")
    private String clientSecret;

    @ApiModelProperty(value = "回调地址", example = "https://jbsh.com/auth/callback")
    private String redirectUri;

    @ApiModelProperty(value = "授权作用域", example = "snsapi_login")
    private String scope;

    @ApiModelProperty(value = "是否已经配置", example = "true")
    private Boolean haveConfig;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean enabled;

    @ApiModelProperty(value = "描述", example = "认证提供者是京博石化")
    private String description;
}
