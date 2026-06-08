package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 认证中心响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("认证中心响应模型")
public class AuthCenterResponseVO {
    @ApiModelProperty(value = "协议类型 系统编码", example = "OAuth2")
    private String protocolType;

    @ApiModelProperty(value = "授权地址", example = "https://auth.supcon.com/auth/authorization")
    private String authUrl;

    @ApiModelProperty(value = "获取token地址", example = "https://auth.supcon.com/auth/access_token")
    private String tokenUrl;

    @ApiModelProperty(value = "凭据最大有时长远", example = "20000")
    private Integer ssoSessionMaxLifespan;

    private int total = 0;

    private int pageSize = 0;

    private int current = 1;

    private List<AuthClientVO> list;
}
