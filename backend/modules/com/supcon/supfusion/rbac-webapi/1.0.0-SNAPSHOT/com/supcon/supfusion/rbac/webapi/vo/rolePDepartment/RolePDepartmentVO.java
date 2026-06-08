package com.supcon.supfusion.rbac.webapi.vo.rolePDepartment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@ApiModel(description= "指定部门返回类")
public class RolePDepartmentVO {

    private static final long serialVersionUID=1L;


    /**
     * 部门ID
     */
    @ApiModelProperty(value = "部门ID")
    private Long id;


}
