package com.supcon.supfusion.rbac.openapi.vo.rolePStaff;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 角色指定人员
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@ApiModel(description= "指定人员返回类")
public class RolePStaffVO {

    private static final long serialVersionUID=1L;

    /**
     * 人员ID
     */
    @ApiModelProperty(value = "人员ID")
    private Long id;



}
