package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class OauthTokenVO extends VO {

    private String accessToken;

    private Integer expiresIn;

    private String refreshToken;

    private Integer refreshExpiresIn;

}
