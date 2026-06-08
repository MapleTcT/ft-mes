package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

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
public class PersonTransferBO extends VO {

    /**
     * 人员id
     */
    private Long id;

    /**
     * 上岗时间
     */
    private Date workTime;

    /**
     * 是否设置成主岗位
     */
    private Boolean mainPosition = false;
}
