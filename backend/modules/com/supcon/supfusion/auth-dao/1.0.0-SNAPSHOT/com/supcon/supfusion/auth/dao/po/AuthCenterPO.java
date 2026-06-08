package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证中心表
 *
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.AUTH_CENTER, autoResultMap = true)
public class AuthCenterPO extends BaseEntity {
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
     * 描述
     */
    private String description;
    /**
     * 是否删除
     */
    private Boolean valid;
}
