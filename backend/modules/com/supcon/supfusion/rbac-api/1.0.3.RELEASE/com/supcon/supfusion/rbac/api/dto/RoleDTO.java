package com.supcon.supfusion.rbac.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDTO extends DTO {


    private static final long serialVersionUID = 2438329873926229942L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 是否有效
     */
    private Long valid;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否叶子
     */
    private Long leaf;

    /**
     * 层级全路径
     */
    private String fullPathName;

    /**
     * 上级节点ID
     */
    private String parentId;

    /**
     * 层级
     */
    private Long layNo;

    /**
     * 层级结构
     */
    private String layRec;

    /**
     * 用于软件公司同步接口
     */
    private String uuid;

    /**
     * 三员类型:1系统管理员,2安全保密员 ,3安全审计员
     */
    private Long threeRoleType;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 排序
     */
    private Long sort;

    /**
     * 描述
     */
    private String description;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 标签
     */
    private List<TagDTO> tags;

    private String createTime;
    private String creator;
    private String modifyTime;
    private String modifier;
}
