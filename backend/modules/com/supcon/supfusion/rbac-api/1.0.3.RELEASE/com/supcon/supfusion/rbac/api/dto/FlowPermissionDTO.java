package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.rbac.api.enums.FlowPermissionType;
import lombok.Data;

/**
 * <p>
 * 工作流数据权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class FlowPermissionDTO{


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;


    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 权限分配来源，3工作流分配的权限
     */
    private Integer purviewDistribution;

    /**
     * 权限的来源：1流程,2开始活动
     */
    private Integer purviewState;

    /**
     * 备注
     */
    private String memo;

    /**
     * 1:无限制
     */
    private Boolean unlimitedPower;

    /**
     * 组限制 0无组限制，1仅组员可见2仅组长可见
     */
    private Boolean groupPowerFlag;

    /**
     * 1:指定人员限制
     */
    private Boolean assignStaffFlag;

    /**
     * 1:指定岗位限制
     */
    private Boolean assignPosFlag;

    /**
     * 1:岗位限制 
     */
    private Boolean positionPowerFlag;

    /**
     * 数据类型 枚举：USER,ROLE,WORKGROUP,DEPTMENT,POSITION
     */
    private FlowPermissionType flowPermissionType;

    /**
     * 根据数据权限类型对应的ID：// USERID，GROUPID，ROLEID，DEPTID，POSITIONID；
     */
    private Long typeId;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 流程版本
     */
    private String flowVersion;

    /**
     * 流程KEY
     */
    private String flowKey;


}
