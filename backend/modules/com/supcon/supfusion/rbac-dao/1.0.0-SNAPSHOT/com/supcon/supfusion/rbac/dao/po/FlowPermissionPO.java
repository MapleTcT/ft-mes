package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;

import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 工作流数据权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_flow_permission", autoResultMap=true)
public class FlowPermissionPO extends BaseEntity {


    private static final long serialVersionUID = -7724056095858432939L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;

    /**
     * 版本
     */
    @TableField("VERSION")
    private Integer version;


    /**
     * 实体编码
     */
    @TableField("ENTITY_CODE")
    private String entityCode;

    /**
     * 权限分配来源，3工作流分配的权限
     */
    @TableField("PURVIEW_DISTRIBUTION")
    private Integer purviewDistribution;

    /**
     * 权限的来源：1流程,2开始活动
     */
    @TableField("PURVIEW_STATE")
    private Integer purviewState;

    /**
     * 备注
     */
    @TableField("MEMO")
    private String memo;

    /**
     * 1:无限制
     */
    @TableField("UNLIMITED_POWER")
    private Boolean unlimitedPower;

    /**
     * 组限制 0无组限制，1仅组员可见2仅组长可见
     */
    @TableField("GROUP_POWER_FLAG")
    private Boolean groupPowerFlag;

    /**
     * 1:指定人员限制
     */
    @TableField("ASSIGN_STAFF_FLAG")
    private Boolean assignStaffFlag;

    /**
     * 1:指定岗位限制
     */
    @TableField("ASSIGN_POS_FLAG")
    private Boolean assignPosFlag;

    /**
     * 1:岗位限制 
     */
    @TableField("POSITION_POWER_FLAG")
    private Boolean positionPowerFlag;

    /**
     * 数据类型 枚举：USER,ROLE,WORKGROUP,DEPTMENT,POSITION
     */
    @TableField("FLOW_PERMISSION_TYPE")
    private FlowPermissionType flowPermissionType;

    /**
     * 根据数据权限类型对应的ID：// USERID，GROUPID，ROLEID，DEPTID，POSITIONID；
     */
    @TableField("TYPE_ID")
    private Long typeId;

    /**
     * 活动编码
     */
    @TableField("ACTIVITY_CODE")
    private String activityCode;

    /**
     * 流程版本
     */
    @TableField("FLOW_VERSION")
    private String flowVersion;

    /**
     * 流程KEY
     */
    @TableField("FLOW_KEY")
    private String flowKey;


}
