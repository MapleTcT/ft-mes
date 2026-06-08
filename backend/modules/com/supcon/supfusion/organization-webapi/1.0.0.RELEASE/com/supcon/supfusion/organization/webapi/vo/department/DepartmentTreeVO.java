package com.supcon.supfusion.organization.webapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

/**
 * 部门树形结构
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTreeVO extends VO {

    /**
     * 部门id
     */
    private Long id;

    /**
     * 部门编码
     */
    private String code;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 统计排序
     */
    private Double sort;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 所属公司id
     */
    private Long companyId;

    /**
     * 是否匹配keyword
     */
    private Boolean match = false;
    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 是否是叶子节点０不是，１是
     */
    private Boolean leaf;


    /**
     * 组织路径
     */
    private String fullPath;

    /**
     * 下级部门
     */
    List<DepartmentTreeVO> children;

    private Integer personNum = 0;
}
