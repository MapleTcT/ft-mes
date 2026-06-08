package com.supcon.supfusion.signature.interapi.vo;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author zhang yafei
 */
@Data
public class CheckUserPasswordVO {

    @ApiModelProperty("是否为首签")
    private Boolean isFirstSigner;

    @ApiModelProperty("用户名")
    @NotBlank(message = "用户名不能为空")
    private String username ;

    @ApiModelProperty("密码")
    @NotBlank(message = "密码不能为空")
    private String password ;

    @ApiModelProperty("按钮code")
    @NotBlank(message = "按钮code不能为空")
    private String buttonCode;

}
