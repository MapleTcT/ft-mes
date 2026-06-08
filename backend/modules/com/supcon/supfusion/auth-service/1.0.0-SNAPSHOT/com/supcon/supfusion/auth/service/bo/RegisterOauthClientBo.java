package com.supcon.supfusion.auth.service.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegisterOauthClientBo {


    @ApiModelProperty("第三方授权地址")
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的授权地址url")
    private String authorizationUri;
    @NotBlank(message = "clientId不能为空")
    @ApiModelProperty("客户端id")
    private String clientId;
    @ApiModelProperty("clientName")
    @NotBlank(message = "clientName不能为空")
    private String clientName;
    @ApiModelProperty("客户端密码")
    @NotBlank(message = "clientSecret不能为空")
    private String clientSecret;
    @NotBlank
    @Pattern(regexp = "(internal|external)")
    @ApiModelProperty(value = "客户端类型",allowableValues = "internal,external")
    private String clientType;
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的登出地址url")
    @ApiModelProperty("登出地址")
    private String logoutUri;
    @ApiModelProperty("lanzuoyun")
    private String protocolType;
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的获取tokenurl")
    @ApiModelProperty("获取token")
    private String tokenUri;
    @Pattern(regexp = "((^$)|(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])", message = "请输入正确的获取用户信息url")
    @ApiModelProperty("获取用户信息地址")
    private String userinfoUri;
    @ApiModelProperty("是否启用")
    private Boolean enable;

}
