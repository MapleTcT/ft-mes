package com.supcon.supfusion.notification.admin.service;


import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMessageUnreadCount;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;


/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:04
 */
public interface NoticeMessageRecordService extends NoticeBaseService<NoticeMsg> {
    /***
     * 消息记录查询
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param protocolId 协议ID
     * @param receiverName 接收者名称
     * @param taskId 任务ID
     * @param sendStatus 发送状态
     * @param readStatus 读取状态
     * @param page 分页器
     * @return
     */
    Page<NoticeMsg> queryPageList(Long startTime, Long endTime, Long protocolId, String receiverName, String taskId, String sendStatus, String readStatus, Page<NoticeMsg> page);

    Page<NoticeMsg> queryStationLetterPage(Long startTime, Long endTime, String readStatus, String staffCode, Page<NoticeMsg> page);

    List<NoticeMsg> queryListByKeyword(Long startTime, Long endTime, Long protocolId, String staffName, String taskId);

    void ackAllStationLetter(Long startTime, Long endTime,String protocol);

    void ackStationLetter(Long startTime, Long endTime, List<String> messageIds,String protocol);

    void ackStationLetters(Long shardingTime, Long id,String protocol);

    long stationLetterUnreadNum();

    Integer countByStaffName(Long startTime, Long endTime, Long protocolId, String staffName, String taskId);

    long getUnreadNum(Long protocolId);

    List<NoticeMessageUnreadCount> getUnreadNumGroup(Long id);


    Page<NoticeMsg> getLatestNewsByTopic(String protocol, Long topicId, Date shardingTime,String staffName,String staffCode,String readStatus,Page<NoticeMsg> page);
}
