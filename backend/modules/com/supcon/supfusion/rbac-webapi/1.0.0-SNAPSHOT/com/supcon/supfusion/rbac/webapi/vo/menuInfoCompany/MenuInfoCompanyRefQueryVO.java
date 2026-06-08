package com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 菜单公司关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
@Data
@ApiModel(description= "菜单公司关联查询返回类")
public class MenuInfoCompanyRefQueryVO implements Serializable {

    private static final long serialVersionUID=1L;


    /**
     * 菜单ID
     */
    @ApiModelProperty(value = "菜单ID")
    private Long menuinfoId;

    /**
     * 公司ID
     */
    @ApiModelProperty(value = "公司ID")
    private Long companyId;

    /**
     * 公司名
     */
    @ApiModelProperty(value = "公司名")
    private String companyName;

    /**
     * 菜单名
     */
    @ApiModelProperty(value = "菜单名")
    private Long menuinfoName;
}
