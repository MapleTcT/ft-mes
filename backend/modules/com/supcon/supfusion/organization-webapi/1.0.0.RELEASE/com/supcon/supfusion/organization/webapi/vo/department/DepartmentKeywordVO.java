package com.supcon.supfusion.organization.webapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 关键词查询
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentKeywordVO extends VO {
    /**
     * 部门id
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 关联的人员数量
     */
    private Long personNum = 0L;
}
