package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;

/**
 * 岗位修改位置VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionLocationVO extends VO {
    /**
     * 岗位id
     */
    @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "岗位id", required = true)
    private Long id;

    /**
     * 前续岗位id
     */
    @ApiModelProperty(value = "前序岗位id")
    private Long upId;

    /**
     * 父级岗位id
     */
    @ApiModelProperty(value = "父级岗位id")
    private Long parentId;
}
