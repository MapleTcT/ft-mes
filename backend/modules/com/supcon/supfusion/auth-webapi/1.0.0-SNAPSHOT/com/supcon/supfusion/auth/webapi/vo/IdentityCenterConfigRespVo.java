package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class IdentityCenterConfigRespVo {

    @ApiModelProperty("pk")
    private Long id;

    @ApiModelProperty("第三方oauth中心名称")
    private String oauthName;

    @ApiModelProperty("协议类型")
    @NotBlank(message = "认证中心必填")
    private String protocolType;

    @ApiModelProperty("应用名")
    private String systemName;
    @ApiModelProperty("内置")
    private Boolean systemFlag;
    @ApiModelProperty("启用")
    private Boolean enable;
    @ApiModelProperty("")
    private String appId;
    @ApiModelProperty("")
    private String appSecret;
    @ApiModelProperty("oauth url")
    private String oauthUrl;
    @ApiModelProperty("获取access tokenurl")
    private String tokenUrl;
    @ApiModelProperty("获取用户信息")
    private String userinfoUrl;
    @ApiModelProperty("扫码登入的url")
    private String qrcodeUrl;
    @ApiModelProperty("登出")
    private String logoutUrl;
    @ApiModelProperty("refresh token ")
    private String refreshUrl;

    private String redirectUrl;

    private String  qrcodeAppid;

    private String description;
}
