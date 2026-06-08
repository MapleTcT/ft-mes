package com.supcon.supfusion.organization.webapi.vo.department;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @ApiModelProperty(value = "部门id", required = true)
    private Long id;

    /**
     * 部门名称
     */
    @Size(min = 1, max = 200, message = Constants.DEPARTMENT_PARAM_NAME_LENGTH_ERROR)
    @ApiModelProperty(value = "部门名称")
    private String name;

    /**
     * 部门类型
     */
    @ApiModelProperty(value = "部门类型")
    private String type;

    /**
     * 描述
     */
    @ApiModelProperty(value = "部门描述")
    @Size(max = 500, message = Constants.DEPARTMENT_PARAM_DESC_LENGTH_ERROR)
    private String description;

    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;

}
