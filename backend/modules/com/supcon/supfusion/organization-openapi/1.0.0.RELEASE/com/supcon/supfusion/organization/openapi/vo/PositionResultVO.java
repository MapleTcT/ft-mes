package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionResultVO extends VO {

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
     * 关联的部门id
     */
    private Long deptId;

    /**
     * 公司信息
     */
    private CompanyResultVO company;
}
