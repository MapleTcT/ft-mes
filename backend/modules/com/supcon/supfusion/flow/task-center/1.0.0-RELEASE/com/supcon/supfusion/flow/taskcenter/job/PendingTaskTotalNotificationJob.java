/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.flow.taskcenter.service.TaskCenterService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.ws.client.NoticeApiClient;
import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;

/**
 * @author: zhuangmh
 * @date: 2021年1月21日 下午6:30:24
 */
@Component
public class PendingTaskTotalNotificationJob extends JobExecutor<Long>{
    
    private static final String TOPIC = "pendingTotal";
    
    @Autowired
    private NoticeApiClient wsNotice;
    @Autowired
    private TaskCenterService taskCenterService;
    /**
     * @see com.supcon.supfusion.flow.taskcenter.job.JobExecutor#submit(java.lang.Object)
     */
    @Override
    public void submit(Long userId) {
        String tenantId = RpcContext.getContext().getTenantId();
        List<NoticeMessageDTO> messages = new ArrayList<>(1);
        JOB_THREAD_POOL.execute(() -> {
            try {
                Thread.sleep(1000); // 延迟1秒通知, 避免幻象读
            } catch (InterruptedException ignore) {

            }
            int total = taskCenterService.queryTotals(userId);
            JSONObject data = new JSONObject();
            data.put("total", total);
            messages.add(new NoticeMessageDTO(userId.toString(), data));
            // 待办总数推送
            wsNotice.pushMessages(TOPIC, tenantId, messages);
        });
    }

}
