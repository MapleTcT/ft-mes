package com.supcon.supfusion.rbac.dao.field;

/**
 * <p>
 * 
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public class RolePermissionField {


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 公司
     */
    public static String cid="CID";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 角色ID
     */
    public static String roleId="ROLE_ID";

    /**
     * 菜单操作ID
     */
    public static String menuOperateId="MENUOPERATE_ID";

    /**
     * 岗位限制
     */
    public static String positionFlag="POSITION_FLAG";

    /**
     * 部门限制
     */
    public static String departmentFlag="DEPARTMENT_FLAG";

    /**
     * 组限制：0 1 2
     */
    public static String groupFlag="GROUP_FLAG";

    /**
     * 指定人员标识
     */
    public static String assignStaffFlag="ASSIGN_STAFF_FLAG";

    /**
     * 指定岗位标识
     */
    public static String assignPosFlag="ASSIGN_POS_FLAG";

    /**
     * 指定部门标识
     */
    public static String assignDeptFlag="ASSIGN_DEPT_FLAG";

    /**
     * 处理人权限：0 1
     */
    public static String dealerPermissionFlag="DEALER_PERMISSION_FLAG";

    /**
     * 无限制
     */
    public static String noRestrictFlag="NO_RESTRICT_FLAG";

    /**
     * 指定业务数据权限限制：0 1
     */
    public static String assignDataPermissionFlag="ASSIGN_DATAPERMISSION_FLAG";

    /**
     * 指定自定义权限
     */
    public static String assignCustomPermissionFlag="ASSIGN_CUSTOMPERMISSION_FLAG";

    /**
     * URL正则
     */
    public static String urlPattern="URL_PATTERN";

}
