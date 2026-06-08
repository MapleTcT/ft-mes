package com.supcon.supfusion.auth.webapi.vo;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoginConfigVO extends VO {

    private Boolean bigSmall;

    private Boolean number;

    private Boolean specialChar;

    @Max(value=32,message = "不大于32")
    @Min(value=8,message = "不小于8")
    private Long min;

    @Max(value=32,message = "不大于32")
    @Min(value=8,message = "不小于8")
    private Long max;
}
