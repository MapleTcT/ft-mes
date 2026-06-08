package com.supcon.supfusion.notification.admin.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.notification.admin.manager.service.IManagerTopicService;
import com.supcon.supfusion.notification.admin.manager.service.IOrganizationStaffService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:52
 */
@Service
public class ManagerTopicServiceImpl implements IManagerTopicService {
    private final static Logger LOGGER = LoggerFactory.getLogger(OrganizationStaffServiceImpl.class);
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;
    @Autowired
    private IOrganizationStaffService organizationStaffService;

    /**
     * 主题新增
     * 解析关联对象
     * 备注 ，接收范围 由manage模块处理
     *
     * @param topic
     * @return
     */
    @Override
    @Transactional
    public NoticeTopic addTopicAndRangeType(NoticeTopic topic) {
        topicService.addEntity(topic);
        //处理接收范围表
        try {
            organizationStaffService.saveReceiveRange(topic.getId(), topic.getReceiveRange());
        } catch (Exception e) {
            LOGGER.error("处理接收范围错误，接收范围实体：{}\n 异常信息：{}--------{}", JSON.toJSON(topic.getReceiveRange()), e.getMessage(), e);
        }
        return topic;
    }

    /**
     * 主题修改
     */
    @Override
    @Transactional
    public NoticeTopic updateTopicAndRangeType(NoticeTopic topic) {
        topic.setCoverSign(0);
        topicService.updateEntity(topic);
        //处理接收范围表
        try {
            //先删除接收范围表
            organizationStaffService.deleteReceiveRange(topic.getId());
            //再构建接收范围表
            organizationStaffService.saveReceiveRange(topic.getId(), topic.getReceiveRange());
        } catch (Exception e) {
            LOGGER.error("修改主题时处理接收范围错误，接收范围实体：{}\n 异常信息：{}--------{}", JSON.toJSON(topic.getReceiveRange()), e.getMessage(), e);
        }
        return topic;
    }
    /**
     * todo 主题查询
     */
    /***
     * todo 主题删除
     */
    @Override
    @Transactional
    public String delTopicAndRangeType(String topicIds) {
        //先删范围
        try {
            organizationStaffService.deleteReceiveRangeByTopicIds(topicIds);
        } catch (Exception e) {
            LOGGER.error("删除主题时处理接收范围错误，主题ids：{}\n 异常信息：{}--------{}", topicIds, e.getMessage(), e);
        }
        topicService.delEntity(topicIds);

        return topicIds;
    }
}
