package com.supcon.supfusion.rbac.openapi.vo.rolePPosition;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 角色指定岗位表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@ApiModel(description= "指定岗位返回类")
public class RolePPositionVO {

    private static final long serialVersionUID=1L;

    /**
     * 岗位ID
     */
    @ApiModelProperty(value = "岗位ID")
    private Long id;


}
