package com.supcon.supfusion.rbac.service.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.po.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.management.relation.Role;
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
public class UserPermissionBO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本信息
     */
    private Integer version;

    /**
     * 公司
     */
    private Long cid;

    /**
     * 授权方式：角色(0)OR用户(1)
     */
    private Integer purviewType;

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
     * 部门限制
     */
    private Boolean departmentFlag;

    /**
     * 指定部门标识
     */
    private Boolean assignDeptFlag;

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
     * 菜单操作ID
     */
    private Long menuOperateId;

    /**
     * 菜单操作ID
     */
    private String menuOperateCode;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色权限与数据权限区分标记：0:流程权限 1：用户权限
     */
    private Long typeFlag = 1L;

    /**
     * 指定的员工
     */
    private List<UserPStaffPO> staffs;
    /**
     * 指定的岗位
     */
    private List<UserPPositionPO> positions;
    /**
     * 指定部门
     */
    private List<UserPDepartmentPO> departments;
    /**
     * 自定义条件
     */
    private List<UserCustomPermissionRefPO> customPermissions;
    /**
     * 数据权限
     */
    private List<UserDataPermissionPO> dataPermissions;

    private List<RolePO> roles;
}
