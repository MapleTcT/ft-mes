package com.supcon.supfusion.rbac.webapi.vo.rolePermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.rbac.webapi.vo.roleCustomPermissionRef.RoleCustomPermissionRefVO;
import com.supcon.supfusion.rbac.webapi.vo.roleDataPermission.RoleDataPermissionVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePDepartment.RolePDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePPosition.RolePPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePStaff.RolePStaffVO;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "角色权限返回类")
public class RolePermissionFindVO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "菜单ID")
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
     * 指定岗位
     */
    @ApiModelProperty(value = "指定岗位")
    private Boolean assignPosFlag;

    /**
     * 岗位限制
     */
    @ApiModelProperty(value = "岗位限制")
    private Boolean positionFlag;


    /**
     * URL正则
     */
    @ApiModelProperty(value = "URL正则")
    private String urlPattern;

    /**
     * 角色ID
     */
    @ApiModelProperty(value = "角色ID")
    private Long roleId;

    /**
     * 菜单操作ID
     */
    @ApiModelProperty(value = "菜单操作ID")
    private Long menuOperateId;

    /**
     * 角色权限与数据权限区分标记：0:数据权限 1：角色权限
     */
    @ApiModelProperty(value = "角色权限与数据权限区分标记")
    private Long typeFlag;
    /**
     * 指定的员工
     */
    @ApiModelProperty(value = "指定的员工")
    private List<RolePStaffVO> staffs;
    /**
     * 指定的岗位
     */
    @ApiModelProperty(value = "指定的岗位")
    private List<RolePPositionVO> positions;
    /**
     * 指定的部门
     */
    @ApiModelProperty(value = "指定的部门")
    private List<RolePDepartmentVO> departments;
    /**
     * 自定义条件
     */
    @ApiModelProperty(value = "自定义条件")
    private List<RoleCustomPermissionRefVO> customPermissions;
    /**
     * 数据权限
     */
    @ApiModelProperty(value = "数据权限")
    private List<RoleDataPermissionVO> dataPermissions;

}
