package com.supcon.supfusion.notification.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTopicListBO;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeTopicService extends NoticeBaseService<NoticeTopic> {

    public Boolean validTopicCode(String code);

    public List<NoticeTopic> queryList(String code, String name, Long topicTree);

    public Page<NoticeTopicListBO> queryPageList(String code, String name, Long topicTree, String protocolIds, String templateName, Integer pageNo, Integer pageSize);

    /***
     * 实体新增时 绑定消息模板
     * @param entity 消息主题实体
     * @return
     */
    @Override
    public NoticeTopic addEntity(NoticeTopic entity);

    @Override
    public NoticeTopic updateEntity(NoticeTopic entity);

    List<String> keywords(String topicCode);

    public List<NoticeTopic> queryListByKeyword(String code, String name, String templateName, String receiver, String topicTreeId);
}
