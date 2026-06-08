package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

/**
 * 岗位详细信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionDetailVO extends VO {

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
     * 上级岗位id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 所属公司id
     */
    private Long companyId;

    /**
     * 关联部门ｉｄ
     */
    private Long depId;

    /**
     * 关联部门名称
     */
    private String depName;

    /**
     * 岗位描述
     */
    private String description;

    /**
     * 管理员
     */
    List<OrganizationManagerBO> managers;

    /**
     * 组织路径
     */
    private String fullPath;

    /**
     * 统计排序
     */
    private Double sort;
}
