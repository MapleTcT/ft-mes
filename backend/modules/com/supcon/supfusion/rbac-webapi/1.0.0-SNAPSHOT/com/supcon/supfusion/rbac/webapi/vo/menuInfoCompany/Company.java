package com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description= "菜单公司关联保存类-公司类")
public class Company {

    @ApiModelProperty(value = "公司ID")
    private Long companyId;

    @ApiModelProperty(value = "公司名")
    private String companyName;
}