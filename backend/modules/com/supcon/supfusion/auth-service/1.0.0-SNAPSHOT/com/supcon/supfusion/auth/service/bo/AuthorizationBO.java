package com.supcon.supfusion.auth.service.bo;

import com.alibaba.fastjson.annotation.JSONField;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class AuthorizationBO extends BO {
    @JSONField(name = Constants.ACCESS_TOKEN)
    private String accessToken;
    @JSONField(name = Constants.REFRESH_TOKEN)
    private String refreshToken;
    @JSONField(name = Constants.TOKEN_TYPE)
    private String tokenType;
    @JSONField(name = Constants.EXPIRES_IN)
    private Integer expiresIn;


    private Long companyId;

    private String tenantId;

    private String errorJSON;

    private String clientId;

    private String userName;

    private String clientAccessToken;

    private String clientRefreshToken;

    // 登录方式,默认为0,表示supOS登录;如果为1,表示竹云单点登录
    private String loginType = "0";

    private String protocolType;

}
