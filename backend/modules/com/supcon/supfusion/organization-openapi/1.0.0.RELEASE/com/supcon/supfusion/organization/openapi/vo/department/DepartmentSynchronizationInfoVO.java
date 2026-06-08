package com.supcon.supfusion.organization.openapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.openapi.vo.person.SystemCodeVO;
import com.supcon.supfusion.organization.openapi.vo.position.CompanyForPositionSynchronizationInfoVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSynchronizationInfoVO extends VO {

    @ApiModelProperty(value = "岗位编码")
    private String code;

    @ApiModelProperty(value = "上级岗位编码")
    private String parentCode;

    @ApiModelProperty(value = "岗位名称")
    private String name;

    @ApiModelProperty(value = "是否有效")
    private Integer valid;

    @ApiModelProperty(value = "修改时间")
    private String modifyTime;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "全路径")
    private String fullPath;

    @ApiModelProperty(value = "层级")
    private Integer layNo;

    @ApiModelProperty(value = "顺序")
    private Double sort;

    @ApiModelProperty(value = "部门类型")
    private SystemCodeVO deptType;

    @ApiModelProperty(value = "所属公司")
    private CompanyForPositionSynchronizationInfoVO company;

    @ApiModelProperty(value = "部门负责人")
    private List<ManagerForDepartmentSynchronizationInfoVO> managers;
}
