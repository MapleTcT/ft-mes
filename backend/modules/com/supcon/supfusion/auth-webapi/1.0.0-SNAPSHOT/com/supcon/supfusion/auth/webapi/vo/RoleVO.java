package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("角色模型")
public class RoleVO extends VO {

    @ApiModelProperty(value = "角色id", example = "11", required = true)
    private Long id;

    @ApiModelProperty(value = "角色id", example = "asdsad", required = true)
    private String name;

    @ApiModelProperty(value = "角色类型", example = "1用户角色 2岗位角色 3 既是用户角色也是岗位角色", dataType = "Long", required = true)
    private Integer type;
}
