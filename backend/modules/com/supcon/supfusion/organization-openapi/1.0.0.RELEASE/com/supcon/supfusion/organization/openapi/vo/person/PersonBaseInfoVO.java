package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonBaseInfoVO extends VO {

    @ApiModelProperty(value = "人员编号")
    private String code;

    @ApiModelProperty(value = "人员姓名")
    private String name;

    @ApiModelProperty(value = "删除标识,0:已删除,1:有效")
    private Integer valid;

    @ApiModelProperty(value = "性别")
    private SystemCodeVO gender;

    @ApiModelProperty(value = "人员状态")
    private SystemCodeVO status;

    @ApiModelProperty(value = "主岗信息")
    private MainPositionBaseVO mainPosition;

    @ApiModelProperty(value = "所属公司信息")
    private List<PersonCompanyBaseVO> companies;

    @ApiModelProperty(value = "所属部门信息")
    private List<PersonDepartmentBaseVO> departments;

    /**
     * 入职时间
     */
    @ApiModelProperty(value = "入职时间")
    private String entryDate;

    /**
     * 职称
     */
    @ApiModelProperty(value = "职称")
    private SystemCodeVO title;

    /**
     * 学历
     */
    @ApiModelProperty(value = "学历")
    private SystemCodeVO education;

    /**
     * 资质
     */
    @ApiModelProperty(value = "资质")
    private String qualification;

    /**
     * 专业
     */
    @ApiModelProperty(value = "专业")
    private String major;

    /**
     * 身份证号
     */
    @ApiModelProperty(value = "身份证号")
    private String idNumber;
}
