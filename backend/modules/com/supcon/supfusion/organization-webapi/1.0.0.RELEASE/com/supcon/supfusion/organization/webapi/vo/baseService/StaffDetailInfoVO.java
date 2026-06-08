package com.supcon.supfusion.organization.webapi.vo.baseService;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.baseService.CompanyBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.DepartmentBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PersonBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PositionBaseServiceBO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 人员相关信息
 */
@Data
public class StaffDetailInfoVO extends VO {

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
