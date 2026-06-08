package com.supcon.supfusion.auth.manager.bo;

import com.alibaba.fastjson.annotation.JSONField;
import com.supcon.supfusion.auth.common.constants.Constants;
import lombok.Data;

/**
 * @author caokele
 */
@Data
public class AuthorizationDTO {
    @JSONField(name = Constants.ACCESS_TOKEN)
    private String accessToken;
    @JSONField(name = Constants.REFRESH_TOKEN)
    private String refreshToken;
    @JSONField(name = Constants.TOKEN_TYPE)
    private String tokenType;
    @JSONField(name = Constants.EXPIRES_IN)
    private Integer expiresIn;
    @JSONField(name = Constants.REFRESH_EXPIRES_IN)
    private Integer refreshExpiresIn;
}
