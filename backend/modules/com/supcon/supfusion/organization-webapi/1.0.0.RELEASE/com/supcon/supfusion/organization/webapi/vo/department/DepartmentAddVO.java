package com.supcon.supfusion.organization.webapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 部门新增的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAddVO extends VO {

    /**
     * 部门编码
     */
    @NotBlank(message = Constants.DEPARTMENT_PARAM_CODE_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.DEPARTMENT_PARAM_CODE_LENGTH_ERROR)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,}$", message = Constants.ORG_CODE_PATTERN)
    @ApiModelProperty(value = "部门编码", required = true)
    private String code;

    /**
     * 部门名称
     */
    @NotBlank(message = Constants.DEPARTMENT_PARAM_NAME_NECESSARY)
    @Size(min = 1, max = 200, message = Constants.DEPARTMENT_PARAM_NAME_LENGTH_ERROR)
    @ApiModelProperty(value = "部门名称", required = true)
    private String name;

    /**
     * 部门类型
     */
    @NotBlank(message = Constants.DEPARTMENT_PARAM_TYPE_NECESSARY)
    @ApiModelProperty(value = "部门类型", required = true)
    private String type;

    /**
     * 所属公司id
     */
    @NotNull(message = Constants.DEPARTMENT_PARAM_COMPANYID_NECESSARY)
    @ApiModelProperty(value = "所属公司id", required = true)
    private Long companyId;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    @ApiModelProperty(value = "父级部门id")
    private Long parentId;

    /**
     * 描述
     */
    @Size(max = 500, message = Constants.DEPARTMENT_PARAM_DESC_LENGTH_ERROR)
    @ApiModelProperty(value = "部门描述")
    private String description;

    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;
}
