package com.supcon.supfusion.organization.service.bo.person;

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
public class PersonOffPositionBO {

    private Long id;
    /**
     * 离岗时间
     */
    private Date offTime;

    /**
     * 调离岗位id
     */
    private List<Long> positionIds;

    /**
     * 主岗id
     */
    private Long mainPositionId;

    private String remark;
}
