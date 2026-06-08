package com.supcon.supfusion.rbac.webapi.vo.role;

import lombok.Data;

import java.util.List;

/**
 * 角色树形结构返回
 * @Author 袁阳
 * @Date 2020-6-10
 */
@Data
public class RoleTreeNoTagVO {

    private String fullName;
    private String fullPath;
    private Integer layNo;
    private Double sort;
    private Long parentId;
    private String description;
    private String shortName;
    private String code;
    private Long id;

    /**
     * 角色信息
     */
    private List<RoleVO> children;
}
