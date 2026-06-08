package com.supcon.supfusion.auth.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
public class UserVO extends VO {

    private String userName;

    private Long userId;

    private Integer userType;

    private Long personId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userDesc;
}
