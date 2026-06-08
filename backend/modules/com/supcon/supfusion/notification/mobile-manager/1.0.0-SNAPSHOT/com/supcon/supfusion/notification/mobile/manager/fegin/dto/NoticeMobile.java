package com.supcon.supfusion.notification.mobile.manager.fegin.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class NoticeMobile {
    private String id;
    private String title;
    private String sender;
    private String content;
    private String url;
    private String topic;
    /**
     * 消息参数
     */
    private String param;
    private Long shardingTime;
}
