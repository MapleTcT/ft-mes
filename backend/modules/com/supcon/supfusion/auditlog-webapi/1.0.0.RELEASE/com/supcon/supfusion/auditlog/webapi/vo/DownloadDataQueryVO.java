package com.supcon.supfusion.auditlog.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 数据模型请求模型
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("导出数据请求模型")
public class DownloadDataQueryVO {

    @ApiModelProperty(value = "是否导出全部", example = "true")
    private Boolean all;

    @ApiModelProperty(value = "链路追踪id，支持多选", example = "[1]")
    private List<Long> traceIds;
}
