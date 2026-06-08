package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.company.CompanyResultBO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionResultBO {

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
    private Long depId;

    /**
     * 公司信息
     */
    private CompanyResultBO company;
}
