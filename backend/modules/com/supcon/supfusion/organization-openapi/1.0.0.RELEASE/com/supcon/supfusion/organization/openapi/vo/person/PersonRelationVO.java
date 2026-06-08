package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.position.PositionDetailInfoVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PersonRelationVO extends VO {

    @ApiModelProperty(value = "人员编号")
    private String code;

    @ApiModelProperty(value = "人员姓名")
    private String name;

    @ApiModelProperty(value = "用户信息")
    private UserVO user;

    @ApiModelProperty(value = "性别")
    private SystemCodeVO gender;

    @ApiModelProperty(value = "人员状态")
    private SystemCodeVO status;

    @ApiModelProperty(value = "主岗信息")
    private MainPositionBaseVO mainPosition;

    @ApiModelProperty(value = "公司信息")
    private List<CompanyDetailInfoVO> companies;

    @ApiModelProperty(value = "部门信息")
    private List<DepartmentDetailInfoVO> departments;

    @ApiModelProperty(value = "岗位信息")
    private List<PositionDetailInfoVO> positions;
}
