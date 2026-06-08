package com.supcon.supfusion.auth.openapi.suposvo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonPropertyOrder({"accessToken", "expiresIn", "refreshToken"})
public class OauthTokenVO extends VO {

    private String accessToken;

    private Integer expiresIn;

    private String refreshToken;

}
