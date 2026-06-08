/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl.operation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.alibaba.druid.util.StringUtils;
import com.supcon.supfusion.flow.common.annotation.OperationType;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.engine.server.service.OperationApi;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:40:54
 */
@OperationType(value = {OperationTypeEnum.REJECT, OperationTypeEnum.REVOKE})
@Component
public class OperationOfRejectOrRevoke implements OperationApi {

    @Autowired
    @Lazy
    private HistoryService historyService;

    /**
     * @see OperationApi#changeRecipient(org.flowable.task.service.delegate.DelegateTask)
     */
    @Override
    public Set<String> changeRecipient(DelegateTask delegateTask) {
        List<HistoricTaskInstance> historyTasks = historyService.createHistoricTaskInstanceQuery()
                .taskDefinitionKey(delegateTask.getTaskDefinitionKey())
                .processInstanceId(delegateTask.getProcessInstanceId())
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .listPage(0, 1);
        if (historyTasks.isEmpty()) {
            return new HashSet<>();
        }
        /**
         * why 'historyTasks.get(0).getAssignee()' ?
         * @see com.supcon.supfusion.flow.engine.server.service.impl.TaskEngineServiceImpl 'taskService.setAssignee(taskId, executor)'
         */
        String assignee = historyTasks.get(0).getAssignee();
        if (StringUtils.isEmpty(assignee)) {
            return new HashSet<>();
        }
        return Collections.singleton(assignee);
    }

}
