package com.supcon.supfusion.notification.app.manager.fegin.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class NoticeADP {
    private String id;
    private String sender;
    private String content;
    private String topic;
    /**
     * 消息参数
     */
    private String param;
    private Long shardingTime;
}
