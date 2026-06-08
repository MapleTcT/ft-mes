package com.supcon.supfusion.organization.webapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CompanyUserVO extends VO {

    /**
     * 管理员用户名
     */
    @ApiModelProperty(value = "公司用户")
    private String userName;

    /**
     * 0普通用户,1系统管理员
     */
    @ApiModelProperty(value = "用户类型,0:普通用户,1:系统管理员")
    private Integer userType;
}
