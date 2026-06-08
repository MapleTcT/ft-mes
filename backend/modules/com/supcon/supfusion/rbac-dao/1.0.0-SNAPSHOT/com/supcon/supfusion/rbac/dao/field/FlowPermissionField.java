package com.supcon.supfusion.rbac.dao.field;

/**
 * <p>
 * 工作流数据权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class FlowPermissionField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="VERSION";


    /**
     * 实体编码
     */
    public static String entityCode="ENTITY_CODE";

    /**
     * 权限分配来源，3工作流分配的权限
     */
    public static String purviewDistribution="PURVIEW_DISTRIBUTION";

    /**
     * 权限的来源：1流程,2开始活动
     */
    public static String purviewState="PURVIEW_STATE";

    /**
     * 备注
     */
    public static String memo="MEMO";

    /**
     * 1:无限制
     */
    public static String unlimitedPower="UNLIMITED_POWER";

    /**
     * 组限制 0无组限制，1仅组员可见2仅组长可见
     */
    public static String groupPowerFlag="GROUP_POWER_FLAG";

    /**
     * 1:指定人员限制
     */
    public static String assignStaffFlag="ASSIGN_STAFF_FLAG";

    /**
     * 1:指定岗位限制
     */
    public static String assignPosFlag="ASSIGN_POS_FLAG";

    /**
     * 1:岗位限制 
     */
    public static String positionPowerFlag="POSITION_POWER_FLAG";

    /**
     * 数据类型 枚举：USER,ROLE,WORKGROUP,DEPTMENT,POSITION
     */
    public static String flowPermissionType="FLOW_PERMISSION_TYPE";

    /**
     * 根据数据权限类型对应的ID：// USERID，GROUPID，ROLEID，DEPTID，POSITIONID；
     */
    public static String typeId="TYPE_ID";

    /**
     * 活动编码
     */
    public static String activityCode="ACTIVITY_CODE";

    /**
     * 流程版本
     */
    public static String flowVersion="FLOW_VERSION";

    /**
     * 流程KEY
     */
    public static String flowKey="FLOW_KEY";


}
