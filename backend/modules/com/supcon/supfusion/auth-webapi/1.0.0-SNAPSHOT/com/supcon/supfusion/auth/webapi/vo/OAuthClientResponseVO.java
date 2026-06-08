package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 认证客户端响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("认证客户端响应模型")
public class OAuthClientResponseVO {
    @ApiModelProperty(value = "主键id", name = "id", example = "580038889177088")
    private Long id;

    @ApiModelProperty(value = "认证中心id", name = "id", example = "680038889177123")
    private Long authCenterId;

    @ApiModelProperty(value = "客户端名称", example = "京博石化")
    private String name;

    @ApiModelProperty(value = "授权模式 系统编码", example = "authorizationCode")
    private SystemCodeResultDTO grantType;

    @ApiModelProperty(value = "clientId", example = "app001")
    private String clientId;

    @ApiModelProperty(value = "客户端密钥", example = "123456")
    private String clientSecret;

    @ApiModelProperty(value = "回调地址", example = "https://jbsh.com/auth/callback")
    private String redirectUri;

    @ApiModelProperty(value = "授权作用域", example = "snsapi_login")
    private String scope;

    @ApiModelProperty(value = "有效期(秒)", example = "7200")
    private Integer expiresIn;

    @ApiModelProperty(value = "认证方式 系统编码", example = "np")
    private SystemCodeResultDTO authMethod;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean enabled;

    @ApiModelProperty(value = "描述", example = "京博石化的客户端")
    private String description;
}
