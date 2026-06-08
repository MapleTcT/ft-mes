package com.supcon.supfusion.rbac.webapi.vo.userPermission;

import com.supcon.supfusion.rbac.webapi.vo.role.RoleVO;
import com.supcon.supfusion.rbac.webapi.vo.userCustomPermissionRef.UserCustomPermissionRefVO;
import com.supcon.supfusion.rbac.webapi.vo.userDataPermission.UserDataPermissionVO;
import com.supcon.supfusion.rbac.webapi.vo.userPDepartment.UserPDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.userPPosition.UserPPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.userPStaff.UserPStaffVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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
@ApiModel(description= "用户权限返回类")
public class UserPermissionFindVO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;


    /**
     * 指定业务数据权限限制：0 1
     */
    @ApiModelProperty(value = "指定业务数据权限限制")
    private Boolean assignDataPermissionFlag;

    /**
     * 指定自定义权限
     */
    @ApiModelProperty(value = "指定自定义权限")
    private Boolean assignCustomPermissionFlag;

    /**
     * 处理人权限：0 1
     */
    @ApiModelProperty(value = "处理人权限")
    private Boolean dealerPermissionFlag;

    /**
     * 无限制
     */
    @ApiModelProperty(value = "无限制")
    private Boolean noRestrictFlag;

    /**
     * 指定人员
     */
    @ApiModelProperty(value = "指定人员")
    private Boolean assignStaffFlag;

    /**
     * 指定岗位
     */
    @ApiModelProperty(value = "指定岗位")
    private Boolean assignPosFlag;

    /**
     * 部门限制
     */
    @ApiModelProperty(value = "部门限制")
    private Boolean departmentFlag;

    /**
     * 指定部门标识
     */
    @ApiModelProperty(value = "指定部门标识")
    private Boolean assignDeptFlag;

    /**
     * 岗位限制
     */
    @ApiModelProperty(value = "岗位限制")
    private Boolean positionFlag;

    /**
     * 组限制：0 1 2
     */
    @ApiModelProperty(value = "组限制")
    private Integer groupFlag;

    /**
     * URL正则
     */
    @ApiModelProperty(value = "URL正则")
    private String urlPattern;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    private Integer purviewType;

    /**
     * 菜单操作ID
     */
    @ApiModelProperty(value = "菜单操作ID")
    private Long menuOperateId;

    /**
     * 菜单操作Code
     */
    @ApiModelProperty(value = "菜单操作Code")
    private String menuOperateCode;

    /**
     * 角色权限与数据权限区分标记：0:数据权限 1：角色权限
     */
    @ApiModelProperty(value = "角色权限与数据权限区分标记")
    private Long typeFlag;
    /**
     * 指定的员工
     */
    @ApiModelProperty(value = "指定的员工")
    private List<UserPStaffVO> staffs;
    /**
     * 指定的岗位
     */
    @ApiModelProperty(value = "指定的岗位")
    private List<UserPPositionVO> positions;
    /**
     * 指定的部门
     */
    @ApiModelProperty(value = "指定的部门")
    private List<UserPDepartmentVO> departments;
    /**
     * 自定义条件
     */
    @ApiModelProperty(value = "自定义条件")
    private List<UserCustomPermissionRefVO> customPermissions;
    /**
     * 数据权限
     */
    @ApiModelProperty(value = "数据权限")
    private List<UserDataPermissionVO> dataPermissions;

    /**
     * 角色
     */
    @ApiModelProperty(value = "角色")
    private List<RoleVO> roles;

}
