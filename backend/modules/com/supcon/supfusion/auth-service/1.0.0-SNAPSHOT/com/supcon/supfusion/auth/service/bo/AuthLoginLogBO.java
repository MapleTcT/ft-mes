package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 登录日志BO类
 *
 * @author kk.c
 */
@Data
public class AuthLoginLogBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名
     */
    private String userName;

    /**
     * 登录IP
     */
    private String loginIp;
    /**
     * 登录时间
     */
    private String loginTime;
    /**
     * 登出时间
     */
    private String logoutTime;
    /**
     * 设备类型
     */
    private String deviceType;
    /**
     * 登录类型：0,表示supOS登录;如果为1,表示第三方登录，具体看protocolType字段
     */
    private String loginType;
    /**
     * 登出类型：0表示主动注销 1表示超时注销 2表示强制退出注销
     */
    private String logoutType;
    /**
     * 用户会话凭证
     */
    private String ticket;
}
