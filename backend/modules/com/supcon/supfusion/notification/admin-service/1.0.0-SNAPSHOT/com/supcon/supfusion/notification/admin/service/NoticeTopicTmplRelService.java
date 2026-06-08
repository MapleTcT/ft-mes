package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;

import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/9 20:38
 */
public interface NoticeTopicTmplRelService  extends NoticeBaseService<NoticeTopicTmplateRelation>{
    /**
     * 新增关联关系
     * @param topic
     * @param template
     * @return
     */
    public NoticeTopicTmplateRelation addEntity(Long topic, Long template, Long protocol);

    public List<NoticeTopicTmplateRelation> addBatchEntity(Long topicId, List<Long> tmpIds);

    /**
     * 根据主题和通知方式查询关联模板
     * @param topic
     * @param protocol
     * @return
     */
    public List<NoticeTopicTmplateRelation> queryEntityByTopic(Long topic, Long protocol);

    /***
     * 根据关联关系表获取实体数据
     * @param topicTmplateRelations
     * @return
     */
    public List<Map<String,Object>> dealRelation(List<NoticeTopicTmplateRelation> topicTmplateRelations);

    public Boolean delByTopic(Long topicId);
}
