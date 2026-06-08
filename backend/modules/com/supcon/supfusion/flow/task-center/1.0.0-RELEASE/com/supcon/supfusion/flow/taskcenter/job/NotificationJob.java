/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.flow.common.dto.NotificationDTO;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.taskcenter.rpc.NotificationService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;

import io.jsonwebtoken.lang.Collections;

/**
 * @author: zhuangmh
 * @date: 2020年6月10日 上午10:14:04
 */
@Component
@Slf4j
public class NotificationJob extends JobExecutor<NotificationDTO> {
    
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private BpmnService bpmnService;
    /**
     * 发送通知
     */
    @Override
    public void submit(NotificationDTO notificationDTO) {
        String tenantId = RpcContext.getContext().getTenantId();
        JOB_THREAD_POOL.execute(() -> {
            RpcContext.getContext().setTenantId(tenantId);
            try {
                switch (notificationDTO.getTopic()) {
                    case TASK_RECEIVE: {
                        String protocols = bpmnService.getUserTaskAttribute(notificationDTO.getTasks().get(0).getInstanceId(), Constants.NOTIFICATION_KEY);
                        if (StringUtils.isEmpty(protocols)) {
                            return;
                        }
                        List<String> protocolList = Collections.arrayToList(protocols.split(Constants.SPLIT_COMMA));
                        for (PendingTaskPO task : notificationDTO.getTasks()) {
                            JSONObject templateParam = buildTemplateParam(task);
                            log.info("发送通知消息内容:{}", templateParam.toString());
                            notificationService.sendTaskReceiveNotice(task.getUserId(), protocolList, templateParam);
                        }
                        break;
                    }
                    case TASK_URGE: {
                        for (PendingTaskPO task : notificationDTO.getTasks()) {
                            JSONObject templateParam = buildUrgeTemplateParam(task);
                            notificationService.sendTaskUrgeNotice(notificationDTO.getUserIds(), notificationDTO.getProtocols(), templateParam);
                        }
                        break;
                    }
                    default: break;
                }
            } finally {
                RpcContext.getContext().setTenantId(null);
            }
        });
    }

    private JSONObject buildTemplateParam(PendingTaskPO task) {
        JSONObject params = new JSONObject();
        params.put(Constants.TASK_RECEIVE_PARAMS_TITLE, task.getProcessName());
        params.put(Constants.TASK_RECEIVE_PARAMS_CONTENT, task.getTaskDescriptionZhCn());
        params.put(Constants.TASK_RECEIVE_PARAMS_EXTENDCONTENT, "");
        String url = String.format("/project/flow/#/workflowPage?todoId=%s&processId=%s&appId=%s", task.getId(), task.getProcessId(), task.getAppId());
        params.put(Constants.TASK_RECEIVE_PARAMS_URL, url);
        // 时区问题先不考虑
        String curDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put(Constants.TASK_RECEIVE_PARAMS_CREATIONTIME, curDate);
        params.put(Constants.TASK_RECEIVE_PARAMS_CREATOR, task.getStaffName());
        return params;
    }
    
    /**
     * 您有一条新催办消息!${title} 
        新催办：${content} 
        ${extendcontent}
         ${url}
        创建人：${creator}
        创建时间：${creationTime}
     */
    private JSONObject buildUrgeTemplateParam(PendingTaskPO task) {
        JSONObject params = new JSONObject();
        params.put(Constants.TASK_RECEIVE_PARAMS_TITLE, task.getProcessName());
        params.put(Constants.TASK_RECEIVE_PARAMS_CONTENT, task.getTaskDescriptionZhCn());
        params.put(Constants.TASK_RECEIVE_PARAMS_EXTENDCONTENT, "");
        String url = String.format("/project/flow/#/workflowPage?todoId=%s&processId=%s&appId=%s", task.getId(), task.getProcessId(), task.getAppId());
        params.put(Constants.TASK_RECEIVE_PARAMS_URL, url);
        // 时区问题先不考虑
        String curDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put(Constants.TASK_RECEIVE_PARAMS_CREATIONTIME, curDate);
        params.put(Constants.TASK_RECEIVE_PARAMS_CREATOR, task.getStaffName());
        return params;
    } 
}
