package com.supcon.supfusion.notification.admin.manager.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 11:13
 */
public interface IManagerTopicService {
    /**
     * 主题新增，调用三方组织架构服务存储接收范围
     * @param topic 主题对象
     * @return
     */
    NoticeTopic addTopicAndRangeType(NoticeTopic topic);
    /**
     * 主题修改，调用三方组织架构服务存储接收范围
     * 先删范围再构建新的范围
     * @param topic 主题对象
     * @return
     */
    NoticeTopic updateTopicAndRangeType(NoticeTopic topic);
    /**
     * 主题删除， 先删范围再删主题对象
     * @param topicIds 主题对象
     * @return
     */
    String delTopicAndRangeType(String topicIds);

}
