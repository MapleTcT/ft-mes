package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentDetailVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDepartmentVO extends VO {

    @ApiModelProperty(value = "所在部门", required = true)
    private DepartmentDetailVO department;

    @ApiModelProperty(value = "顶层部门", required = true)
    private DepartmentDetailVO rootDepartment;
}
