package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionDTO extends DTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司
     */
    private Long cid;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单操作ID
     */
    private Long menuOperateId;

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
