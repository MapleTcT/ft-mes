package com.supcon.supfusion.rbac.webapi.vo.rolePermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.rbac.webapi.vo.rolePDepartment.RolePDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePPosition.RolePPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePStaff.RolePStaffVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "角色权限基础")
public class RolePermissionBaseVO {
    private static final long serialVersionUID = -9032713351677106142L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;


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
     * 岗位限制
     */
    @ApiModelProperty(value = "岗位限制")
    private Boolean positionFlag;

    /**
     * 部门限制
     */
    @ApiModelProperty(value = "部门限制")
    private Boolean departmentFlag;

    /**
     * 组限制：0 1 2
     */
    @ApiModelProperty(value = "组限制")
    private Boolean groupFlag;

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
     * 指定部门
     */
    @ApiModelProperty(value = "指定部门")
    private Boolean assignDeptFlag;

    /**
     * 指定人员数据
     */
    @ApiModelProperty(value = "指定人员数据")
    private List<RolePStaffVO> staffs;

    /**
     * 指定岗位数据
     */
    @ApiModelProperty(value = "指定岗位数据")
    private List<RolePPositionVO> positions;

    /**
     * 指定部门数据
     */
    @ApiModelProperty(value = "指定部门数据")
    private List<RolePDepartmentVO> departments;

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
     * 指定自定义权限
     */
    @ApiModelProperty(value = "指定自定义权限")
    private Boolean assignCustomPermissionFlag;

    /**
     * 指定业务数据权限限制：0 1
     */
    @ApiModelProperty(value = "指定业务数据权限限制")
    private Boolean assignDataPermissionFlag;

    /**
     * URL正则
     */
    @ApiModelProperty(value = "URL正则")
    private String urlPattern;


}
