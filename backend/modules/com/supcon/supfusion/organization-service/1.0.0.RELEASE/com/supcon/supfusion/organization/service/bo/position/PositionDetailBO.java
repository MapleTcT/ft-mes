package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

/**
 * 岗位PO详情类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionDetailBO {
    /**
     * 岗位id
     */
    private Long id;
    /**
     * 岗位编码
     */
    private String code;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 是否为主岗
     */
    private Boolean mainPosition;
    /**
     * 所属公司id
     */
    private Long companyId;

    /**
     * 关联的部门id
     */
    private Long depId;

    /**
     * 关联的部门名称
     */
    private String depName;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 描述
     */
    private String description;


    /**
     * 岗位层级
     */
    private Integer layNo;

    /**
     * 岗位全路径
     */
    private String fullPath;

    /**
     * 顺序
     */
    private Double sort;

    /**
     * 是否是叶子节点０不是，１是
     */
    private Boolean leaf;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;

    /**
     * 上级岗位的编码
     */
    private String parentCode;

    private List<Long> roleIds;

    private String layRec;

}
