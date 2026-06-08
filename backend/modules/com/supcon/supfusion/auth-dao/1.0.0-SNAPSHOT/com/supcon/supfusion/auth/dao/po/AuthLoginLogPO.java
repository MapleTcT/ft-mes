package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/**
 * 登录日志PO类
 *
 * @author kk.c
 */
@Data
@TableName(value = Constants.AUTH_LOGIN_LOG, autoResultMap = true)
public class AuthLoginLogPO extends BaseEntity {
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
    @TableField(fill = FieldFill.INSERT, jdbcType = JdbcType.TIMESTAMP, typeHandler = UTCToStringTypeHandler.class)
    private String loginTime;
    /**
     * 登出时间
     */
    @TableField(fill = FieldFill.UPDATE, jdbcType = JdbcType.TIMESTAMP, typeHandler = UTCToStringTypeHandler.class)
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
