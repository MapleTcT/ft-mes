package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 在线用户响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("在线用户响应模型")
public class OnlineUserResponseVO {
    @ApiModelProperty(value = "主键id", name = "id", example = "580038889177088")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "280038889177082")
    private Long userId;

    @ApiModelProperty(value = "用户名称", example = "zhangsan")
    private String userName;

    @ApiModelProperty(value = "人员ID", example = "580038889177088")
    private Long personId;

    @ApiModelProperty(value = "人员名称", example = "张三")
    private String personName;

    @ApiModelProperty(value = "人员编码", example = "0120190305")
    private String personCode;

    @ApiModelProperty(value = "登录IP", example = "xxx.xxx.xxx.xxx")
    private String loginIp;

    @ApiModelProperty(value = "登录时间", example = "2020-08-03T21:02:02.000+0000")
    private String loginTime;
}
