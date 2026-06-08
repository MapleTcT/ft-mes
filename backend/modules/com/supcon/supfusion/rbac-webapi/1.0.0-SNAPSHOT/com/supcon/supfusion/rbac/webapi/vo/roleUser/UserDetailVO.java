package com.supcon.supfusion.rbac.webapi.vo.roleUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description= "用户信息")
public class UserDetailVO {

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "用户名")
    private String userName;

    @ApiModelProperty(value = "人员名")
    private String personName;

    @ApiModelProperty(value = "人员编码")
    private String personCode;
}
