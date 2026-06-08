package com.supcon.supfusion.organization.openapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.SystemCodeVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentBaseInfoVO extends VO {

    @ApiModelProperty(value = "部门编码")
    private String code;

    @ApiModelProperty(value = "部门名称")
    private String name;

    @ApiModelProperty(value = "上级部门编码")
    private String parentCode;

    @ApiModelProperty(value = "部门类型")
    private SystemCodeVO deptType;

    @ApiModelProperty(value = "是否有效")
    private Integer valid;

    @ApiModelProperty(value = "部门全路径")
    private String fullPath;

    @ApiModelProperty(value = "所属公司")
    private CompanyBaseInfoVO company;
}
