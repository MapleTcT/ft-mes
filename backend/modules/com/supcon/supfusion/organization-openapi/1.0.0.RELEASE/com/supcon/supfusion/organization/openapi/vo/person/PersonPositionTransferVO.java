package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人员调入岗位
 *
 * @author shidongsheng
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonPositionTransferVO extends VO {

    /**
     * 调入的岗位id
     */
    @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "岗位id", required = true)
    private Long positionId;

    /**
     * 调入岗位的人员id
     */
    @ApiModelProperty(value = "岗位调入人员信息", required = false)
    private List<PersonTransferVO> persons;
}
