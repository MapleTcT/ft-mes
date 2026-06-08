package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class SaveIdentityCenterConfigVo {


    @ApiModelProperty("应用名")
    @NotBlank(message = "应用名必填")
    @Size(max = 50, min = 1, message = "应用名最大50位")
    private String systemName;
    @ApiModelProperty("第三方oauth中心名称")
    @NotBlank(message = "认证中心必填")
    private String oauthName;
    @ApiModelProperty("协议类型")
    @NotBlank(message = "认证中心必填")
    private String protocolType;
    @ApiModelProperty("")
    @NotBlank(message = "客户端id必填")
    private String appId;
    @NotBlank(message = "客户端密码必填")
    @ApiModelProperty("")
    private String appSecret;


    @ApiModelProperty("oauth url")
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的授权地址url")
    @Size(max = 256)
    private String oauthUrl;


    @ApiModelProperty("获取access tokenurl")
    @Size(max = 256)
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的tokenUrl")
    private String tokenUrl;

    @ApiModelProperty("获取redirectUrl")
    @Size(max = 256)
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的tokenUrl")
    private String redirectUrl;


    @ApiModelProperty("获取用户信息")
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的获取用户信息url")
    @Size(max = 256)
    private String userinfoUrl;

    @ApiModelProperty("扫码登入的url")
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的扫码登入url")
    @Size(max = 256)
    private String qrcodeUrl;
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的登出url")
    @Size(max = 256)
    @ApiModelProperty("登出")
    private String logoutUrl;
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的授权地址url")
    @Size(max = 256)
    @ApiModelProperty("refresh token")
    private String refreshUrl;

    private String description;
    @ApiModelProperty("内置")
    private Boolean systemFlag;

    private String  qrcodeAppid;
}
