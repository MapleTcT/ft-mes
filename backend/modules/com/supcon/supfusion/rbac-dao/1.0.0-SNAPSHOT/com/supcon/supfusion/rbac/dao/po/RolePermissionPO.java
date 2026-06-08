package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;

import javax.management.relation.Role;
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
@Data
@TableName(value = "rbac_rolepermission", autoResultMap=true)
public class RolePermissionPO extends BaseEntity implements Serializable {


    private static final long serialVersionUID = 3197623628696111707L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;

    /**
     * 公司
     */
    @TableField("CID")
    private Long cid;

    /**
     * 版本
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 角色ID
     */
    @TableField("ROLE_ID")
    private Long roleId;

    /**
     * 菜单操作ID
     */
    @TableField("MENUOPERATE_ID")
    private Long menuOperateId;

    /**
     * 岗位限制
     */
    @TableField("POSITION_FLAG")
    private Boolean positionFlag;

    /**
     * 部门限制
     */
    @TableField("DEPARTMENT_FLAG")
    private Boolean departmentFlag;

    /**
     * 组限制：0 1 2
     */
    @TableField("GROUP_FLAG")
    private Integer groupFlag;

    /**
     * 指定人员标识
     */
    @TableField("ASSIGN_STAFF_FLAG")
    private Boolean assignStaffFlag;

    /**
     * 指定岗位标识
     */
    @TableField("ASSIGN_POS_FLAG")
    private Boolean assignPosFlag;

    /**
     * 指定部门标识
     */
    @TableField("ASSIGN_DEPT_FLAG")
    private Boolean assignDeptFlag;

    /**
     * 指定人员数据
     */
    @TableField(exist = false)
    private List<RolePStaffPO> staffs;

    /**
     * 指定岗位数据
     */
    @TableField(exist = false)
    private List<RolePPositionPO> positions;

    /**
     * 指定部门数据
     */
    @TableField(exist = false)
    private List<RolePDepartmentPO> departments;

    /**
     * 处理人权限：0 1
     */
    @TableField("DEALER_PERMISSION_FLAG")
    private Boolean dealerPermissionFlag;

    /**
     * 无限制
     */
    @TableField("NO_RESTRICT_FLAG")
    private Boolean noRestrictFlag;

    /**
     * 指定业务数据权限限制：0 1
     */
    @TableField("ASSIGN_DATAPERMISSION_FLAG")
    private Boolean assignDataPermissionFlag;

    /**
     * 指定自定义权限
     */
    @TableField("ASSIGN_CUSTOMPERMISSION_FLAG")
    private Boolean assignCustomPermissionFlag;

    /**
     * URL正则
     */
    @TableField("URL_PATTERN")
    private String urlPattern;

}
