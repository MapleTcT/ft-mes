package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 组新增的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupAddVO extends VO {
    /**
     * 组编码
     */
    @NotBlank(message = Constants.GROUP_PARAM_CODE_NECESSARY)
    private String code;

    /**
     * 组名称
     */
    @NotBlank(message = Constants.GROUP_PARAM_NAME_NECESSARY)
    private String name;


    /**
     * 所属公司id
     */
    @NotNull(message = Constants.GROUP_PARAM_COMPANYID_NECESSARY)
    private Long companyId;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人id
     */
    private Long managerId;

    /**
     * 负责人名称
     */
    private String managerName;
}
