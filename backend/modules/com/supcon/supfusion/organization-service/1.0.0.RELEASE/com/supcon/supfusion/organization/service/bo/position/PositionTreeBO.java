package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import lombok.*;

import java.util.List;

/**
 * 岗位树形PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionTreeBO {
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
     * 统计排序
     */
    private Double sort;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 所属公司id
     */
    private Long companyId;

    private Long depId;

    /**
     * 是否匹配keyword
     */
    private Boolean match = false;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 前继岗位
     */
    private PositionTreeBO prePos;

    /**
     * 下级岗位
     */

    /**
     * 是否是叶子节点０不是，１是
     */
    private Boolean leaf;

    /**
     * 岗位全路径
     */
    private String fullPath;

    /**
     * 孩子岗位
     */
    List<PositionTreeBO> children;

    DepartmentDetailBO department;
}
