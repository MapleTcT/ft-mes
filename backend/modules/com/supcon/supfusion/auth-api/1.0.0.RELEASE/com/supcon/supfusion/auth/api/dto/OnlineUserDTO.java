package com.supcon.supfusion.auth.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 在线用户
 *
 * @author caokele
 */
@Data
public class OnlineUserDTO {

    @NotEmpty(message = "用户会话凭证不能为空")
    private String ticket;

    @NotEmpty(message = "登录IP不能为空")
    private String loginIp;

    @NotNull(message = "用户Id不能为空")
    private Long userId;

    private Long companyId;
}
