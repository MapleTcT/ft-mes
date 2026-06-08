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

/**
 * 部门修改位置VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentLocationVO extends VO {

    /**
     * 部门id
     */
    @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "部门id", required = true)
    private Long id;

    /**
     * 前续部门id
     */
    @ApiModelProperty(value = "移动后的前序部门id")
    private Long upId;

    /**
     * 父级部门id
     */
    @ApiModelProperty(value = "父级部门id")
    private Long parentId;

}
