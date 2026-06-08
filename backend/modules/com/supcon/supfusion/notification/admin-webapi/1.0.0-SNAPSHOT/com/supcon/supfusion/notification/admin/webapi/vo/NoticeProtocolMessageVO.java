package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeProtocolMessageVO {
    @ApiModelProperty(value = "消息表ID")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long id;

    @ApiModelProperty(value = "人员编码")
    private String staffCode;

    @ApiModelProperty(value = "人员名称")
    private String staffName;

    @ApiModelProperty(value = "发送状态", notes = "0失败，1成功，2 未知，3待查询(只有状态3才会触发回执查询)，9已分发")
    private Integer sendStatus;

    @ApiModelProperty(value = "发送失败返回结果")
    private String errorResult;

    @ApiModelProperty(value = "阅读状态", notes = "0未读，1已读，2未知(只有状态0才会触发回执查询)")
    private Integer readStatus;

    @ApiModelProperty(value = "失败重试次数")
    private Integer retry;

    @ApiModelProperty(value = "消息发送时间戳")
    private Long shardingTime;

    @ApiModelProperty(value = "发送任务表ID")
    private String noticeTaskId;

    @ApiModelProperty(value = "协议表ID")
    private String noticeProtocolId;

    @ApiModelProperty(value = "发送任务协议表ID")
    private String noticeTaskProtocolId;
    @ApiModelProperty(value = "消息主题名称")
    private String topic;
    @ApiModelProperty(value = "业务方事务编号")
    private String bsmodCode;
    @ApiModelProperty(value = "业务模块名称")
    private String bsmodName;
    @ApiModelProperty(value = "用户名")
    private String userName;
    @ApiModelProperty(value = "用户名")
    private String sender;

    @ApiModelProperty(value = "业务参数")
    private String param;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "创建时间")
    private Long createTime;
}
