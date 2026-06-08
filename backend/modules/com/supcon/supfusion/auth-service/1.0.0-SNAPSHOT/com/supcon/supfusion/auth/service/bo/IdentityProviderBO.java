package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 身份提供者
 *
 * @author caokele
 */
@Data
public class IdentityProviderBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 身份提供商名称
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
     * 客户端id
     */
    private String clientId;
    /**
     * 客户端密钥
     */
    private String clientSecret;
    /**
     * 回调地址
     */
    private String redirectUri;
    /**
     * 授权作用域
     */
    private String scope;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 描述
     */
    private String description;
    /**
     * 是否删除
     */
    private Boolean valid;
}
