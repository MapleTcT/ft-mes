package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 在线用户
 *
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.AUTH_ONLINE_USER, autoResultMap = true)
public class OnlineUserPO extends BaseEntity {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 用户Id
     */
    private Long userId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 人员ID
     */
    private Long personId;
    /**
     * 人员编号
     */
    private String personCode;
    /**
     * 人员名称
     */
    private String personName;
    /**
     * 企业Id
     */
    private Long companyId;
    /**
     * 用户会话凭证
     */
    private String ticket;
    /**
     * 登录IP
     */
    private String loginIp;

    private String deviceType;

    /**
     * 登录时间
     */
    @TableField(
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String loginTime;
}
