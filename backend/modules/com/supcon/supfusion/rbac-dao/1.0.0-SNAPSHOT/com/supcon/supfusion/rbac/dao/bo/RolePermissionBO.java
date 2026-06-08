package com.supcon.supfusion.rbac.dao.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.rbac.dao.po.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class RolePermissionBO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 指定业务数据权限限制：0 1
     */
    private Boolean assignDataPermissionFlag;

    /**
     * 指定自定义权限
     */
    private Boolean assignCustomPermissionFlag;

    /**
     * 处理人权限：0 1
     */
    private Boolean dealerPermissionFlag;

    /**
     * 无限制
     */
    private Boolean noRestrictFlag;

    /**
     * 指定人员
     */
    private Boolean assignStaffFlag;

    /**
     * 指定岗位
     */
    private Boolean assignPosFlag;

    /**
     * 岗位限制
     */
    private Boolean positionFlag;

    /**
     * 组限制：0 1 2
     */
    private Integer groupFlag;

    /**
     * URL正则
     */
    private String urlPattern;
    /**
     * 部门限制
     */
    private Boolean departmentFlag;

    /**
     * 指定部门标识
     */
    private Boolean assignDeptFlag;

    /**
     * 菜单操作ID
     */
    private Long menuOperateId;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色
     */
    private RolePO role;

    private String flowKey;

    /**
     * 角色权限与数据权限区分标记：0:流程权限 1：角色权限
     */
    private Long typeFlag = 1L;
    /**
     * 指定的员工
     */
    private List<RolePStaffPO> staffs;
    /**
     * 指定部门
     */
    private List<RolePDepartmentPO> departments;
    /**
     * 指定的岗位
     */
    private List<RolePPositionPO> positions;
    /**
     * 自定义条件
     */
    private List<RoleCustomPermissionRefPO> customPermissions;
    /**
     * 数据权限
     */
    private List<RoleDataPermissionPO> dataPermissions;
}
