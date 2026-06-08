package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * @Author kk.C
 * @Description: 邮箱验证码方式修改密码参数VO类
 * @Date 2021/3/5 15:12
 */
@Data
public class UpdatePwdBO {

    private String verificationCode;
    private String password;
    private String personCode;
    private Long userId;
}
