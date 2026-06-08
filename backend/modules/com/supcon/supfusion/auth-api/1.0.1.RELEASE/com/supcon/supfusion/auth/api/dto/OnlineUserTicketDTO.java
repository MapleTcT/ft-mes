package com.supcon.supfusion.auth.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 在线用户凭证
 *
 * @author caokele
 */
@Data
public class OnlineUserTicketDTO {
    /**
     * 用户会话凭证
     */
    @NotEmpty(message = "用户会话凭证不能为空")
    private String ticket;
    /**
     * 旧用户会话凭证
     */
    @NotEmpty(message = "旧用户会话凭证不能为空")
    private String oldTicket;
    /**
     * 会话凭证过期时间
     */
    @NotNull(message = "会话凭证过期时间不能为空")
    private String ticketExpireTime;
}
