package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:23
 */
public interface NoticeReceiveRangeService extends NoticeBaseService<NoticeRecieveRange> {
    List<NoticeRecieveRange> queryListByTopic(Long topicId);
    Boolean deleteListByTopic(Long topicId);
}
