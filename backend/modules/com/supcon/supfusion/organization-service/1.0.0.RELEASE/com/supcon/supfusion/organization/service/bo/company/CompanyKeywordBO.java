package com.supcon.supfusion.organization.service.bo.company;

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
public class CompanyKeywordBO {
    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司简称名称
     */
    private String shortName;

    /**
     * 公司全称
     */
    private String fullName;

    /**
     * 编码
     */
    private String code;

    /**
     * 关联的人员数量
     */
    //private Long personNum = 0L;
}
