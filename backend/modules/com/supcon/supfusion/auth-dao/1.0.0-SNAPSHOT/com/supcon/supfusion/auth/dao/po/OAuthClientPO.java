package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证客户端表
 *
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.AUTH_OAUTH_CLIENT, autoResultMap = true)
public class OAuthClientPO extends BaseEntity {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 认证中心id
     */
    private Long authCenterId;
    /**
     * 客户端名称
     */
    private String name;
    /**
     * 授权模式 系统编码
     */
    private String grantType;
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
     * 有效期
     */
    private Integer expiresIn;
    /**
     * 认证方式 系统编码
     */
    private String authMethod;
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
