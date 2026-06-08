package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位树形结构
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionTreeVO extends VO {
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

    /**
     * 是否匹配keyword
     */
    private Boolean match = false;
    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 下级岗位
     */
    List<PositionTreeVO> children;

    public List<PositionTreeVO> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }
}
