package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.Date;
import java.util.List;

/**
 * 岗位调离
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonOffPositionVO extends VO {

    @ApiModelProperty(value = "人员id", required = true)
    private Long id;
    /**
     * 离岗时间
     */
    @ApiModelProperty(value = "离岗时间", required = true)
    private Date offTime;

    /**
     * 备注
     */
    @ApiModelProperty(value = "离岗备注", required = false)
    private String remark;

    /**
     * 调离岗位id
     */
    @ApiModelProperty(value = "调离岗位id", required = true)
    private List<Long> positionIds;

    @ApiModelProperty(value = "设置新的主岗id", required = false)
    private Long mainPositionId;

}
