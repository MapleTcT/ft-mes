package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author kk.C
 * @Description: 邮箱验证码方式修改密码参数VO类
 * @Date 2021/3/5 15:12
 */
@Data
@ApiModel("邮箱验证码方式修改密码参数VO类")
public class UpdatePwdVO {

    @NotBlank(message = "缺少验证码参数")
    private String verificationCode;
    @NotBlank(message = "缺少新密码参数")
    private String password;
    @NotBlank(message = "缺少人员编码参数")
    private String personCode;
    @NotNull(message = "缺少用户ID参数")
    private Long userId;
}
