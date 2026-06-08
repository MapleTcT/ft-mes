package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 在线用户
 *
 * @author caokele
 */
@Data
public class OnlineUserBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 用户Id
     */
    private Long userId;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 人员ID
     */
    private Long personId;
    /**
     * 人员名称
     */
    private String personName;
    /**
     * 人员编码
     */
    private String personCode;
    /**
     * 用户会话凭证
     */
    private String ticket;
    /**
     * 登录IP
     */
    private String loginIp;
    /**
     * 登录时间
     */
    private String loginTime;
    /**
     * 企业ID
     */
    private Long companyId;

    private String deviceType;

}
