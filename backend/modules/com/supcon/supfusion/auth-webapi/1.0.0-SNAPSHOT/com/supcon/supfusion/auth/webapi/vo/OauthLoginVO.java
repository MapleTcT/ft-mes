package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
public class OauthLoginVO extends VO {

    private String grantType;

    private String code;

    private String clientId;

    private String redirectUri;
}

