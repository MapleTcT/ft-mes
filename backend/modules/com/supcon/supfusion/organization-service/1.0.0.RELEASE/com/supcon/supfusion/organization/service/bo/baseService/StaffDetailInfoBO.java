package com.supcon.supfusion.organization.service.bo.baseService;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 人员相关信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StaffDetailInfoBO {

    /**
     * 员工信息
     */
    @ApiModelProperty(value = "人员")
    private PersonBaseServiceBO staff;

    /**
     * 公司信息
     */
    @ApiModelProperty(value = "公司信息")
    private CompanyBaseServiceBO company;

    /**
     * 员工信息
     */
    @ApiModelProperty(value = "部门信息")
    private DepartmentBaseServiceBO department;

    /**
     * 员工信息
     */
    @ApiModelProperty(value = "主岗信息")
    private PositionBaseServiceBO mainPosition;


}
