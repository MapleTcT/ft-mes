package com.supcon.supfusion.iam.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:50
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountVO extends VO {
    private static final long serialVersionUID = -3359430208335123035L;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String username;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;

    @JsonProperty("ak")
    private String accessKey;

    @JsonProperty("sk")
    private String secretKey;
}
