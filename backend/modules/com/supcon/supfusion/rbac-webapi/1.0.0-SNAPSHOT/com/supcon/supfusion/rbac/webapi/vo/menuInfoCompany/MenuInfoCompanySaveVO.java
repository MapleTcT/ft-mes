package com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


@Data
@ApiModel(description= "菜单公司关联保存类")
public class MenuInfoCompanySaveVO extends VO {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "菜单ID")
    private Long menuinfoId;

    @ApiModelProperty(value = "公司列表")
    private List<Company> companies;

}
