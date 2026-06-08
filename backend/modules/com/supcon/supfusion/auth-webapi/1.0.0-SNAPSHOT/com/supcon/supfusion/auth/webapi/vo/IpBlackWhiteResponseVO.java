package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * ip黑白名单响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("ip黑白名单响应模型")
public class IpBlackWhiteResponseVO {
    @ApiModelProperty(value = "主键id", name = "id", example = "580038889177088")
    private Long id;

    @ApiModelProperty(value = "访问IP", example = "xxx.xxx.xx.xxx")
    private String ip;

    @ApiModelProperty(value = "管控模式 0:黑名单 1:白名单", example = "0")
    private Integer controlType;

    @ApiModelProperty(value = "加入时间", example = "2020-08-03T21:02:02.000+0000")
    private String createTime;
}
