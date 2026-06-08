package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class IdentityCenterConfigVo {

    @ApiModelProperty("pk")
    private Long id;

    @ApiModelProperty("oauthName")
    private String oauthName;


    @ApiModelProperty("应用名")
    private String systemName;
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

    private String description;

    @ApiModelProperty("是否启用")
    private Boolean enable;
}
