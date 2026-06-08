package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 部门修改的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentUpdateVO extends VO {

    /**
     * 部门id
     */
    @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY)
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门类型
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;
}
