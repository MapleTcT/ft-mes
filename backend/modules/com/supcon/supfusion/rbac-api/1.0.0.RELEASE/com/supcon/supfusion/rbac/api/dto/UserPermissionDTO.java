package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
public class UserPermissionDTO implements Serializable {


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
     * 用户ID
     */
    private Long userId;

    /**
     * 菜单操作ID
     */
    private Long menuOperateId;

    /**
     * 菜单操作编码
     */
    private String menuOperateCode;

    /**
     * 授权方式：角色(0)OR用户(1)
     */
    private Integer purviewType;

    /**
     * 岗位限制
     */
    private Boolean positionFlag;

    /**
     * 部门限制
     */
    private Boolean departmentFlag;

    /**
     * 组限制：0 1 2
     */
    private Integer groupFlag;

    /**
     * 指定人员标识
     */
    private Boolean assignStaffFlag;

    /**
     * 指定岗位标识
     */
    private Boolean assignPosFlag;

    /**
     * 指定部门标识
     */
    private Boolean assignDeptFlag;


    /**
     * 处理人权限：0 1
     */
    private Boolean dealerPermissionFlag;

    /**
     * 无限制
     */
    private Boolean noRestrictFlag;

    /**
     * 指定业务数据权限限制：0 1
     */
    private Boolean assignDataPermissionFlag;

    /**
     * 指定自定义权限
     */
    private Boolean assignCustomPermissionFlag;

    /**
     * URL正则
     */
    private String urlPattern;

}
