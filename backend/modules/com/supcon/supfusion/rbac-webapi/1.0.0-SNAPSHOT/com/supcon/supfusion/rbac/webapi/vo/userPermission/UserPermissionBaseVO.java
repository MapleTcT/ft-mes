package com.supcon.supfusion.rbac.webapi.vo.userPermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.rbac.webapi.vo.userPDepartment.UserPDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.userPPosition.UserPPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.userPStaff.UserPStaffVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "用户权限基础")
public class UserPermissionBaseVO {
    private static final long serialVersionUID = -9032713351677106142L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;


    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    /**
     * 公司ID
     */
    @ApiModelProperty(value = "公司ID")
    private Long cid;

    /**
     * 菜单操作ID
     */
    @ApiModelProperty(value = "菜单操作ID")
    private Long menuOperateId;

    /**
     * 菜单操作编码
     */
    @ApiModelProperty(value = "菜单操作编码")
    private String menuOperateCode;

    /**
     * 授权方式：角色(0)OR用户(1)
     */
    @ApiModelProperty(value = "授权方式")
    private Integer purviewType;

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
    private List<UserPStaffVO> staffs;

    /**
     * 指定岗位数据
     */
    @ApiModelProperty(value = "指定岗位数据")
    private List<UserPPositionVO> positions;

    /**
     * 指定岗位数据
     */
    @ApiModelProperty(value = "指定岗位数据")
    private List<UserPDepartmentVO> departments;

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
