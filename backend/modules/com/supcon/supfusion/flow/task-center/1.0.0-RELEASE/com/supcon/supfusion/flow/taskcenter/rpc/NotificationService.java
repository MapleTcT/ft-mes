/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.dto.NotificationDTO;
import com.supcon.supfusion.flow.common.enumeration.NotificationTopicEnum;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.flow.taskcenter.job.NotificationJob;
import com.supcon.supfusion.module.registry.ModuleEnum;
import com.supcon.supfusion.notification.admin.api.NoticeTopicApi;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;
import com.supcon.supfusion.notification.apiserver.api.SendNoticeV2InternalApi;
import com.supcon.supfusion.notification.apiserver.api.dto.RangeDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithTopicRequestDTO;
import com.supcon.supfusion.notification.common.bean.RangeType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年9月10日 下午3:58:54
 */
@Service
@Slf4j
public class NotificationService {
    
    @Autowired
    private SendNoticeV2InternalApi noticeService;
    @Autowired
    private NoticeTopicApi noticeTopicApi;
    @Autowired
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private NotificationJob notificationJob;
    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    
    /**
     * 待办创建发送通知
     * @param processId
     */
    public void sendNoticeIfConfigure(String processId) {
        // 从redis中取出待发送通知的任务
        String taskIds = redisUtils.getStringValue(processId);
        if (StringUtils.isEmpty(taskIds)) {
            return;
        }
        String[] taskIdArray = taskIds.split(Constants.SPLIT_COMMA);
        List<PendingTaskPO> tasks = pendingTaskMapper.selectList(new QueryWrapper<PendingTaskPO>().lambda().in(PendingTaskPO::getId, Arrays.asList(taskIdArray)));
        if (tasks.isEmpty()) {
            return;
        }
        notificationJob.submit(new NotificationDTO(NotificationTopicEnum.TASK_RECEIVE, tasks, null, null));
    }
    
    /**
     * 给单个待办
     * @param task
     */
    public void sendNoticeIfConfigure(List<PendingTaskPO> tasks) {
        notificationJob.submit(new NotificationDTO(NotificationTopicEnum.TASK_RECEIVE, tasks, null, null));
    }
    
    /**
     * 发送待办接收通知
     * 模板: 您有一条新待办消息!${title} 
        新待办：${content} 
        ${extendcontent} 
        ${url}
        创建人：${creator}
        创建时间：${creationTime}
     * @param userId 用户ID
     * @param protocols 发送方式
     * @param params 
     *          模板参数 变量包含${title}, ${content}, ${extendcontent}, ${url}, ${creator}, ${creationTime}         
     */
    public void sendTaskReceiveNotice(Long userId, List<String> protocols, JSONObject params) {
        // 用户ID转为人员ID
        Set<String> staffCodes = new HashSet<>();
        UserOrgDetailDTO user = userServiceAdapter.getUserById(userId);
        if (user != null && user.getPersonId() != null) {
            staffCodes.add(user.getPersonCode());
        }
        log.info("待办邮件发送人员: {}", String.join(Constants.SPLIT_COMMA, staffCodes));
        try {
            SendWithTopicRequestDTO messageContent = buildSendWithTopicRequest(params, staffCodes, protocols, Constants.TASK_RECEIVE_TOPIC);
            noticeService.topic(messageContent);
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }
    
    /**
     * 发送催办通知
     * 您有一条新催办消息!${title} 
       新催办：${content} 
       ${extendcontent}
       ${url}
       创建人：${creator}
       创建时间：${creationTime}
     * @param userIds 用户ID
     * @param protocols 发送方式
     * @param params 
     *          模板参数 变量包含${title}, ${content}, ${extendcontent}, ${url}, ${creator}, ${creationTime}              
     */
    public void sendTaskUrgeNotice(Set<Long> userIds, List<String> protocols, JSONObject param) {
        // 用户ID转为人员ID
        Set<String> staffCodes = new HashSet<>();
        for (Long userId : userIds) {
            UserOrgDetailDTO user = userServiceAdapter.getUserById(userId);
            if (user != null && user.getPersonId() != null) {
                staffCodes.add(user.getPersonCode());
            }
        }
        log.info("催办邮件发送人员: {}", String.join(Constants.SPLIT_COMMA, staffCodes));
        try {
            SendWithTopicRequestDTO messageContent = buildSendWithTopicRequest(param, staffCodes, protocols, Constants.TASK_URGE_TOPIC);
            noticeService.topic(messageContent);
        } catch (Exception e) {
            log.error("催办通知失败", e);
        }
        
    }
    
    private SendWithTopicRequestDTO buildSendWithTopicRequest(JSONObject param, Collection<String> staffCodes, List<String> protocols, String topic) {
        SendWithTopicRequestDTO sendRequest = new SendWithTopicRequestDTO();
        sendRequest.setBsmodCode(ModuleEnum.WORKFLOW.getModuleId());
        sendRequest.setBsmodName(ModuleEnum.WORKFLOW.getModuleId());
        sendRequest.setTopicCode(topic);
        sendRequest.setProtocols(protocols);
        sendRequest.setParam(param);
        // 设置发送人员
        RangeDTO range = new RangeDTO();
        range.setCodes(staffCodes);
        range.setRangeType(RangeType.STAFF);
        sendRequest.setReceivers(Collections.singletonList(range));
        return sendRequest;
    }
    
    /**
     * 获取主题通知方式
     * @param topicCode
     * @return
     */
    public List<ProtocolDTO> retrieveProtocols(String topicCode) {
        try {
            return noticeTopicApi.getTopicProtocols(topicCode);
        } catch (Exception e) {
            log.error("获取主题支持通知方式失败", e);
        }
        return new ArrayList<>(1);
    }
}
