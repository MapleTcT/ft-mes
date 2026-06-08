package com.supcon.supfusion.notification.admin.service.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内信配置实体
 *
 * @author huangxin2
 * @create 2020/5/20 19:03
 */
@Getter
@Setter
public class NoticeStationLetterConfig {
    //展示模式
    @JsonIgnore
   private String showMode ="展示模式";
   //警铃消息

    private StationLetterAlarmLetter alarmLetter;
    //气泡消息
    @Getter
    @Setter
    private StationLetterBubbleLetter bubbleLetter;
    //模态窗口
    @Getter
    @Setter
    private StationLetterShowWindow showdowWindow;
    //重试时间
    @Getter
    @Setter
    private String retryInterval;
    //重试次数
    @Getter
    @Setter
    private Integer retryTime;

}
