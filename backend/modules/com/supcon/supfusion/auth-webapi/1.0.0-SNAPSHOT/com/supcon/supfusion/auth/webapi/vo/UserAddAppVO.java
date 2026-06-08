package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserAddAppVO extends VO {
    @NotBlank(message = "用户名称必填")
    private String userName;
    @NotEmpty(message = "密码不能为空")
    private String password;
    @Range(min = 0, max = 2, message = "用户类型0 或者1 2")
    private Integer userType;
}
