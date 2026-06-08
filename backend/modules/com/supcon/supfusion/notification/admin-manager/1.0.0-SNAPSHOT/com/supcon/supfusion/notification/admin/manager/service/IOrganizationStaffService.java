package com.supcon.supfusion.notification.admin.manager.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;

import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 11:13
 */
public interface IOrganizationStaffService {
    /**
     * 保存接收范围表和接收范围扩展表数据
     * @param topicId
     * @param receiveRangeMap
     */
    void saveReceiveRange(Long topicId, List<Map<String, List<Object>>> receiveRangeMap);

    /***
     * 查询接收范围表和接收范围扩展表数据
     * @param topicId
     * @return
     */
    Map<String, List<NoticeRecieveRangeExt>> queryReceiveRange(Long topicId);

    /**
     * 根据主题ID删除接收范围
     * @param topicId
     * @return
     */
    List<NoticeRecieveRange> deleteReceiveRange(Long topicId);
    List<NoticeRecieveRange> deleteReceiveRangeByTopicIds(String topicIds);
}
