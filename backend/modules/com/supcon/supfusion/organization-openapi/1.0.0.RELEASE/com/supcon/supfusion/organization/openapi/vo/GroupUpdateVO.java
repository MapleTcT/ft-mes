package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 组修改po
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateVO extends VO {

    /**
     * 组id
     */
    @NotNull(message = Constants.GROUP_PARAM_ID_NECESSARY)
    private Long id;

    /**
     * 组名称
     */
    @NotBlank(message = Constants.GROUP_PARAM_NAME_NECESSARY)
    private String name;

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
