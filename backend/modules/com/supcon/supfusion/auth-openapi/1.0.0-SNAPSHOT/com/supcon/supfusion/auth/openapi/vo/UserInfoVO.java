package com.supcon.supfusion.auth.openapi.vo;

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
public class UserInfoVO extends VO {
    private String userName;
    private String personCode;
    private String companyCode;
    private Integer userType;
}
