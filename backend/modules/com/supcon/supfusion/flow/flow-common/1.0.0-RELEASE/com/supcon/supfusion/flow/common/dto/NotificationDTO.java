/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.List;
import java.util.Set;

import com.supcon.supfusion.flow.common.enumeration.NotificationTopicEnum;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月10日 下午7:58:27
 */
@Data
@AllArgsConstructor
public class NotificationDTO {
    /**
     * 发送主题  
     */
    private NotificationTopicEnum topic;
    /**
     * 待办任务
     */
    private List<PendingTaskPO> tasks;
    /**
     * 需要发送通知的用户
     */
    private Set<Long> userIds; 
    /**
     * 发送协议
     */
    private List<String> protocols;
}
