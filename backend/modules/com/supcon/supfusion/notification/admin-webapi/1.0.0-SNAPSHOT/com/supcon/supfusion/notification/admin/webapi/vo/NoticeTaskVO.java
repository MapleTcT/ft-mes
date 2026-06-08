package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NoticeTaskVO {

    @ApiModelProperty(value = "任务ID")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long id;

    @ApiModelProperty(value = "发送方编号")
    private String bsmodCode;

    @ApiModelProperty(value = "服务名称")
    private String bsmodName;

    @ApiModelProperty(value = "任务类型")
    private Integer taskType;

    @ApiModelProperty(value = "任务状态")
    private Integer status;

    @ApiModelProperty(value = "分表时间戳")
    private Long shardingTime;

    @ApiModelProperty(value = "消息主题ID")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long noticeTopicId;

    @ApiModelProperty(value = "多种通知方式名称")
    private String protocolNames;
}
