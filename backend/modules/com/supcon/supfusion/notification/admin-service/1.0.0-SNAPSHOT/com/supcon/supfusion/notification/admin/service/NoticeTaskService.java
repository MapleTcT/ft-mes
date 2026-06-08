package com.supcon.supfusion.notification.admin.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeTaskService extends NoticeBaseService<NoticeTask>{
    /***
     * 分页条件查询任务对象
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param noticeTopicId 消息主题ID
     * @param bsmodCode 发送方编号
     * @param bsmodName 服务名称
     * @param page 分页对象
     * @return
     */
    public Page<NoticeTask> queryTaskPage(String startTime, String endTime, String id, Long noticeTopicId, String bsmodCode, String bsmodName, Page<NoticeTask> page);
    
    public List<NoticeTask> queryListByKeyword(String startTime, String endTime,String id ,Long noticeTopicId,String bsmodCode,String bsmodName);

    String getContent(Long noticeTaskProtocolId);
}
