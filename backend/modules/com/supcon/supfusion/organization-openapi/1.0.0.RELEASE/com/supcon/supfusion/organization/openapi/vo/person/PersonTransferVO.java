package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Date;

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
public class PersonTransferVO extends VO {

    /**
     * 人员id
     */
    @ApiModelProperty(value = "人员id", required = true)
    private Long id;

    /**
     * 上岗时间
     */
    @ApiModelProperty(value = "上岗时间", required = true)
    private Date workTime;

    /**
     * 是否设置成主岗位
     */
    @ApiModelProperty(value = "是否是主岗", required = false)
    private Boolean mainPosition = false;
}
