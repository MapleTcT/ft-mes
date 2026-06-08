/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.exception.ConfigureException;
import com.supcon.supfusion.flow.common.exception.StatusAbnormalException;
import com.supcon.supfusion.flow.common.exception.TaskRuntimeException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.engine.server.service.TaskEngineService;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年6月5日 上午9:28:35
 */
@Service
@Slf4j
public class TaskEngineServiceImpl implements TaskEngineService {

    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    
    /**
     * @see TaskEngineService#complete(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.util.Map)
     */
    @Override
    public void complete(String taskInstanceId, String processInstanceId, String userId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        // 开启忽略人工环节
        enableUserTaskSkipExpression(transientVariables);
        String assignee = getAssignee(taskInstanceId);
        taskService.setAssignee(taskInstanceId, userId);
        try {
            taskService.complete(taskInstanceId, variables, transientVariables);
        } catch (Exception e) {
            if (assignee == null) {
                taskService.deleteUserIdentityLink(taskInstanceId, userId, IdentityLinkType.ASSIGNEE);
            } else {
                taskService.setAssignee(taskInstanceId, assignee);
            }
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof BizException) {
                throw (BizException)rootCause;
            }
            log.error("待办提交异常", e);
            throw new TaskRuntimeException(FlowErrorEnum.TASK_COMPLETE_FAIL, e);
        } finally {
            List<ProcessInstance> processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).active().listPage(0, 1);
            if (!processInstance.isEmpty()) {
                Set<String> trashyKeys = transientVariables.keySet().stream()
                        .filter(k -> (!Constants.FORM_DATA.equals(k) && !Constants.FORM_NO.equals(k)))
                        .collect(Collectors.toSet());
                runtimeService.removeVariables(processInstanceId, trashyKeys);
            }
        }
    }
    
    private String getAssignee(String taskInstanceId) {
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskInstanceId);
        for (IdentityLink identityLink : identityLinks) {
            if (IdentityLinkType.ASSIGNEE.equals(identityLink.getType())) {
                return identityLink.getUserId();
            }
        }
        return null;
    }

    private void enableUserTaskSkipExpression(Map<String, Object> transientVariables) {
        transientVariables.put(Constants.SKIP_EXPRESSION_ENABLED_VARIABLE, Boolean.TRUE);
    }

    /**
     * @see TaskEngineService#detectAutoComplete(java.lang.String)
     */
    @Override
    public boolean detectAutoComplete(String taskInstanceId) {
        List<RecipientRuleDTO> rules = bpmnService.queryRecipientRules(taskInstanceId);
        if (rules.size() == 1) {
            return rules.get(0).getRecipientSelect() == RecipientSelection.INITIATOR;
        }
        return false;
    }

    /**
     * @see TaskEngineService#revoke(java.lang.String, java.lang.String, java.util.Set, java.util.Map)
     */
    @Override
    public void revoke(String taskInstanceId, String processInstanceId, Set<String> currentUserTaskKeys, Map<String, Object> engineVariables) {
        HistoricTaskInstance hisTask = historyService.createHistoricTaskInstanceQuery().taskId(taskInstanceId).singleResult();
        // 查询当前进行中的待办实例以便回收, 使得流程回到上一个环节
        List<String> taskDefinitionKeys = new LinkedList<>();
        for (String currentUserTaskKey : currentUserTaskKeys) {
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(currentUserTaskKey).list();
            if (!tasks.isEmpty()) {
                List<String> tks = tasks.stream().map(Task :: getTaskDefinitionKey).collect(Collectors.toList());
                taskDefinitionKeys.addAll(tks);
            }
        }
        if (taskDefinitionKeys.isEmpty()) {
            return;
        }
        try {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .processVariables(engineVariables)
                .moveActivityIdsToSingleActivityId(taskDefinitionKeys, hisTask.getTaskDefinitionKey())
                .changeState();
            historyService.deleteHistoricTaskInstance(taskInstanceId);
        } finally {
            // 清除流程上下文变量
            List<ProcessInstance> processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).active().listPage(0, 1);
            if (!processInstance.isEmpty()) {
                runtimeService.removeVariables(processInstanceId, engineVariables.keySet());
            }
        }
    }
    

    /**
     * @see TaskEngineService#joinMultiTask(java.lang.String, java.util.List, java.util.Map)
     */
    @Override
    public List<String> joinMultiTask(String taskInstanceId, List<String> invitees, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskInstanceId).singleResult();
        if (task.isSuspended()) {
            throw new StatusAbnormalException(FlowErrorEnum.TASK_STATUS_NOT_ALLOW_JOIN_ERROR);
        }
        // 验证是否开启加签功能
        if (detectDisabled(taskInstanceId)) {
            throw new ConfigureException(FlowErrorEnum.TASK_JOIN_NOT_ENABLE_ERROR);
        }
        List<String> newInvitees = runtimeService.getVariable(task.getProcessInstanceId(), task.getTaskDefinitionKey(), List.class);
        if (newInvitees == null) {
            newInvitees = new ArrayList<>(invitees.size());
        }
        List<String> taskIds = new ArrayList<>();
        for (String invitee : invitees) {
            newInvitees.add(invitee);
            variables.put(IdentityLinkType.ASSIGNEE, invitee);
            Execution execution = runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), task.getProcessInstanceId(), variables);
            Task t = taskService.createTaskQuery().executionId(execution.getId()).singleResult();
            taskIds.add(t.getId());
        }
        // 驳回时需要重新加上这些人
        runtimeService.setVariable(task.getProcessInstanceId(), task.getTaskDefinitionKey(), newInvitees);
        return taskIds;
    }
    
    private boolean detectDisabled(String taskInstanceId) {
        String joinflag = bpmnService.getUserTaskAttribute(taskInstanceId, Constants.ENABLE_ADDINSTANCE);
        return (Constants.DISABLED + "").equals(joinflag);
    }

    /**
     * @see TaskEngineService#migrate(String, String, String)
     */
    @Override
    public String migrate(String processInstanceId, String from, String to) {
        List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(from).list();
        try {
            runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstanceId)
            .moveActivityIdTo(from, to)
            .processVariable(Constants.DISABLE_TASK_EVENT, Constants.ENABLED) // 待办实例的合并不会触发待办中心的数据变更
            .changeState();
        } finally {
            runtimeService.removeVariable(processInstanceId, Constants.DISABLE_TASK_EVENT);
        }
        list.forEach(task -> {
            historyService.deleteHistoricTaskInstance(task.getId());
        });
        List<Task> combinedTask = taskService.createTaskQuery()
                .taskDefinitionKey(to)
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime().desc()
                .listPage(0, 1);
        return combinedTask.get(0).getId();
    }
}    
