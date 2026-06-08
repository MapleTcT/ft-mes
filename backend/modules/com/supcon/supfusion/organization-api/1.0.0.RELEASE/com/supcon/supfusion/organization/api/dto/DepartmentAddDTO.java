package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 部门新增类
 * @author root
 *
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAddDTO extends DTO {
    /**
     * 部门编码
     */
    //@NotBlank(message = Constants.DEPARTMENT_PARAM_CODE_NECESSARY)
    private String code;

    /**
     * 部门名称
     */
    //@NotBlank(message = Constants.DEPARTMENT_PARAM_NAME_NECESSARY)
    private String name;

    /**
     * 部门类型
     */
    //@NotBlank(message = Constants.DEPARTMENT_PARAM_TYPE_NECESSARY)
    private String type;

    /**
     * 所属公司id
     */
    //@NotNull(message = Constants.DEPARTMENT_PARAM_COMPANYID_NECESSARY)
    private Long companyId;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人id
     */
    //@ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;
}
