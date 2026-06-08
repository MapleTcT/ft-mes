package com.supcon.supfusion.organization.openapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ManagerForDepartmentSynchronizationInfoVO extends VO {

    @ApiModelProperty(value = "负责人编号")
    private String code;

    @ApiModelProperty(value = "负责人姓名")
    private String name;
}
