package com.supcon.supfusion.organization.openapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionChildVO extends VO {

    @ApiModelProperty(value = "上级岗位编码")
    private String parentCode;

    @ApiModelProperty(value = "岗位编码")
    private String code;

    @ApiModelProperty(value = "岗位名称")
    private String name;

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

    @ApiModelProperty(value = "所属部门")
    private DepartmentForPositionSynchronizationInfoVO department;

    @ApiModelProperty(value = "所属公司")
    private CompanyForPositionSynchronizationInfoVO company;

    @ApiModelProperty(value = "岗位关联的角色")
    private List<PositionRoleBaseVO> roles;
}
