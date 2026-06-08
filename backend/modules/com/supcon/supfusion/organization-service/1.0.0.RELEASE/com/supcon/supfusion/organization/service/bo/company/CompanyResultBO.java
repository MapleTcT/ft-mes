package com.supcon.supfusion.organization.service.bo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResultBO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司编码
     */
    private String code;

    /**
     * 集团或公司简称
     */
    private String shortName;

    /**
     * 集团或公司全称
     */
    private String fullName;

    /**
     * 公司全路径
     */
    private String fullPath;

    /**
     * 父级节点id
     */
    private Long parentId;
}
