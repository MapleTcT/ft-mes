package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 岗位修改的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionUpdateVO extends VO {

    /**
     * 岗位id
     */
    @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "岗位id", required = true)
    private Long id;

    /**
     * 岗位名称
     */
    @Size(min = 1, max = 200, message = Constants.POSITION_PARAM_NAME_LENGTH_ERROR)
    @ApiModelProperty(value = "岗位名称")
    private String name;


    /**
     * 描述
     */
    @Size(max = 500, message = Constants.POSITION_PARAM_DESC_LENGTH_ERROR)
    @ApiModelProperty(value = "岗位描述")
    private String description;

    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;

    @ApiModelProperty(value = "岗位关联的部门id", required = true)
    private Long depId;
}
