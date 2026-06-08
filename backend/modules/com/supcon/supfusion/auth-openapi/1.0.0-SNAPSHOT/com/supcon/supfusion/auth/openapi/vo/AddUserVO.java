package com.supcon.supfusion.auth.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;


@Setter
@Getter
@ToString
public class AddUserVO extends VO {

    @NotBlank(message = "用户名必填")
    private String username;

    @NotBlank(message = "密码必填")
    private String password;

    @NotBlank(message = "人员编码必填")
    private String personCode;

    @NotBlank(message = "公司编码必填")
    private String companyCode;

    @Range(min = 1,max = 1,message = "userType只能为0")
    private Integer accountType;

    @Length(max = 512,message = "用户描述最大512个字符")
    private String userDesc;

    @Pattern(regexp="GMT[+-]?[1-9]?[12]?",message = "时区格式不对")
    private String timeZone;

    private List<String> roleNameList;

}
