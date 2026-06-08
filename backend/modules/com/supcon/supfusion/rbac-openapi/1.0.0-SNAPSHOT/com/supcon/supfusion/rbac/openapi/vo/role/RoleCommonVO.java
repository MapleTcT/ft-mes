package com.supcon.supfusion.rbac.openapi.vo.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.*;

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
@ApiModel(description= "角色类")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleCommonVO extends VO {

    private static final long serialVersionUID = -766803994355192366L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否叶子
     */
    private Boolean leaf;

    /**
     * 层级全路径
     */
    private String fullPathName;

    /**
     * 上级节点ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer layNo;

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
    private Integer threeRoleType;

    /**
     * 角色类型
     * SystemCode:
     *  ROLE_TYPE/roletype 1 默认公司
     */
    private String roleType;

    /**
     * 排序
     */
    private Double sort;

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
     * 版本
     */
    private Integer version;

}
