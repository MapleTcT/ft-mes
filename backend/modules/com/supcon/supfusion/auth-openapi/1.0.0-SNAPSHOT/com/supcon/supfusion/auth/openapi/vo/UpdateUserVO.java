package com.supcon.supfusion.auth.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

@Setter
@Getter
@ToString
public class UpdateUserVO extends VO {

    @Length(max = 512,message = "用户描述最大512个字符")
    private String userDesc;

    @Pattern(regexp="GMT[+-]?[1-9]?[12]?",message = "时区格式不对")
    private String timeZone;

    private String personCode;

    private Integer lockStatus;
}
