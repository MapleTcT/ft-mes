package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 岗位新增的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionPersonVO extends VO {

    /**
     * 岗位ｉｄ
     */
    @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "岗位id", required = true)
    private Long positionId;
    /**
     * 关联人员id
     */
    @ApiModelProperty(value = "人员id集合", required = false)
    private List<Long> persons;
}
