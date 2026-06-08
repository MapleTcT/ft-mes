package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResultVO extends VO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司编码
     */
    @NotBlank(message = Constants.COM_PARAM_CODE_NOTNULL)
    private String code;

    /**
     * 集团或公司简称
     */
    @NotBlank(message = Constants.COM_PARAM_SHORTNAME_NOTNULL)
    private String shortName;

    /**
     * 集团或公司全称
     */
    @NotBlank(message = Constants.COM_PARAM_FULLNAME_NOTNULL)
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
