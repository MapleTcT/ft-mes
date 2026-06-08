package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeTopicTreeService extends NoticeBaseService<NoticeTopicTree> {
    public List<NoticeTopicTree> queryListByKeyword(String keyword);
    public List<NoticeTopicTree> queryListByNameOrCode(String code, String name);
    public String deleteById(String id);
}
