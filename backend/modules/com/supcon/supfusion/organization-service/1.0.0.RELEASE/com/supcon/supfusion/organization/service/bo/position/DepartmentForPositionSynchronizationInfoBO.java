package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentForPositionSynchronizationInfoBO {

    private String code;

    private String name;
}
