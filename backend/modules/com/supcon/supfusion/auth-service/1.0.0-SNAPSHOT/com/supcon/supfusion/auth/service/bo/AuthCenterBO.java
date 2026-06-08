package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 认证中心
 *
 * @author caokele
 */
@Data
public class AuthCenterBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 认证中心名称
     */
    private String name;
    /**
     * 协议类型 系统编码
     */
    private String protocolType;
    /**
     * 授权地址
     */
    private String authUrl;
    /**
     * 获取token地址
     */
    private String tokenUrl;
    /**
     * 获取用户信息地址
     */
    private String profileUrl;
    /**
     * 客户端数量
     */
    private Integer clientNumber;
    /**
     * 描述
     */
    private String description;
    /**
     * 是否删除
     */
    private Boolean valid;
}
