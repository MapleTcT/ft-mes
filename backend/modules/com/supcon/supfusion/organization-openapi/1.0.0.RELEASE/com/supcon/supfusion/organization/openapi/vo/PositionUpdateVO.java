package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
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
    private Long id;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 岗位类型
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

    /**
     * 关联部门id
     */
    private Long depId;

    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;
}
