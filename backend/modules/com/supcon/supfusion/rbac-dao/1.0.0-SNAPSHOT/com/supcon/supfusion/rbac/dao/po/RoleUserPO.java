package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 角色用户表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_roleuser", autoResultMap = true)
public class RoleUserPO extends LogicDeleteBaseEntity {
    private static final long serialVersionUID = 8489743919055047537L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本信息
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 是否仅是岗位带入的角色
     */
    @TableField("POSITION_FLAG")
    private Boolean positionFlag;

    /**
     * 角色ID
     */
    @TableField("ROLE_ID")
    private Long roleId;

    /**
     * 用户ID
     */
    @TableField("USER_ID")
    private Long userId;


    /**
     * 调出时间
     */
    @TableField("END_TIME")
    private String endTime;

    /**
     * 调入时间
     */
    @TableField("START_TIME")
    private String startTime;

    /**
     * 更新时间
     */
//    @TableField("MODIFY_TIME")
//    private String modifyTime;

    /**
     * 用户名
     */
    @TableField("USER_NAME")
    private String userName;

    /**
     * 人员名
     */
    @TableField("PERSON_NAME")
    private String personName;

    /**
     * 人员编码
     */
    @TableField("PERSON_CODE")
    private String personCode;

    /**
     * 来源 1 来源于用户 2 来源于岗位 3 两者都有
     */
    @TableField("FROM_POSITION")
    private Integer fromPosition;

    /**
     * 角色
     */
    @TableField(exist = false)
    private RolePO role;

    public static String getRoleIdFeildName() {
        return "ROLE_ID";
    }

    public static String getValidFeildName() {
        return "VALID";
    }

    public static String getUserIdFeildName() {
        return "USER_ID";
    }
}
