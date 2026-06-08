package com.supcon.supfusion.iam.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AKSKVO extends VO {
    @ApiModelProperty(value = "ID")
    private Long id;
    @ApiModelProperty(value = "APP_ID")
    private String appId;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "创建时间")
    private Long createTime;
    @ApiModelProperty(value = "是否能操作。0 能操作，1 不能操作")
    private Integer system;
    @ApiModelProperty(value = "是否能下载。0 能下载，1 不能下载")
    private Integer downLoadMark;
}