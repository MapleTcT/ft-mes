package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.rbac.dao.po.UserPDepartmentPO;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.dao.po.UserPStaffPO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public class UserPermissionField implements Serializable {


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本信息
     */
    public static String version= "VERSION";

    /**
     * 公司
     */
    public static String cid="CID";

    /**
     * 用户ID
     */
    public static String userId="USER_ID";

    /**
     * 菜单操作ID
     */
    public static String menuOperateId="MENUOPERATE_ID";

    /**
     * 菜单操作编码
     */
    public static String menuOperateCode="MENUOPERATE_CODE";

    /**
     * 授权方式：角色(0)OR用户(1)
     */
    public static String purviewType="PURVIEW_TYPE";

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
