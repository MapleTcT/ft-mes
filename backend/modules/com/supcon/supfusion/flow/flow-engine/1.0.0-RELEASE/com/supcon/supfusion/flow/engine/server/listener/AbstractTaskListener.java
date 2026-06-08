/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.listener;


import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.supcon.supfusion.flow.common.dto.TaskDTOAdapter;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.engine.server.register.OperationContext;
import com.supcon.supfusion.flow.engine.server.service.OperationApi;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午3:17:23
 */
public abstract class AbstractTaskListener implements TaskListener {

    private static final long serialVersionUID = 1L;
    @Autowired
    private transient RuntimeService runtimeService;
    @Autowired
    private transient RepositoryService repositoryService;
    @Autowired
    private transient OperationContext operationContext;
    
    @Override
    public void notify(DelegateTask task) {
        Integer disabled = runtimeService.getVariable(task.getExecutionId(), Constants.DISABLE_TASK_EVENT, Integer.class);
        if (disabled != null) {
            return;
        }
        switch (task.getEventName()) {
            case EVENTNAME_CREATE: {
                UserTask userTask = getUserTaskNode(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
                Set<String> specialUsers = getSpecialUser(task, userTask);
                createPendingTask(transfer(task, specialUsers, userTask));
                break;
            }
            case EVENTNAME_COMPLETE: case EVENTNAME_DELETE: {
                deletePendingTask(task.getId());
                break;
            }
            default: break;
        }
    }
    
    // 针对指派,撤回等情况
    private Set<String> getSpecialUser(DelegateTask delegateTask, UserTask userTask) {
        String multipleInstance = userTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.MULTIPLE_INSTANCE);
        // 会签的执行者人选已经在AbstractMultipleTaskListener确定
        if (Boolean.valueOf(multipleInstance)) {
            return null;
        }
        OperationTypeEnum recipientOperation = LocalContext.getContext().getOperationType();
        // 撤回 驳回 手动指派, 不同类型重新指派处理方式不同
        OperationApi recipientOperator = operationContext.getInstance(recipientOperation);
        if (recipientOperator != null) {
            Set<String> changedRecipients = recipientOperator.changeRecipient(delegateTask);
            if (!changedRecipients.isEmpty()) {
                return changedRecipients;
            }
        }
        return null;
    }
    
    private UserTask getUserTaskNode(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
        return (UserTask)flowElement;
    }
    
    /**
     * 往待办中心创建待办
     * @param taskDTOAdapter
     */
    public abstract void createPendingTask(TaskDTOAdapter taskDTOAdapter);
    /**
     * 删除待办
     * @param taskInstanceId
     */
    public abstract void deletePendingTask(String taskInstanceId);
    
    private TaskDTOAdapter transfer(DelegateTask delegateTask, Set<String> specialUsers, UserTask userTask) {
        String multipleInstance = userTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.MULTIPLE_INSTANCE);
        String protocols = userTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.NOTIFICATION_KEY);
        String pageUrl =  userTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.PAGE_URL);
        String formNo = runtimeService.getVariable(delegateTask.getExecutionId(), Constants.FORM_NO, String.class);
        String formData = runtimeService.getVariable(delegateTask.getExecutionId(), Constants.FORM_DATA, String.class);
        // 外部指定的流程名称
        String processName = runtimeService.getVariable(delegateTask.getExecutionId(), Constants.PROCESS_NAME, String.class);
        // 流程发起者
        String initiator = runtimeService.getVariable(delegateTask.getExecutionId(), Constants.PROCESS_INITIATOR, String.class);
        Integer enableDelete = runtimeService.getVariable(delegateTask.getExecutionId(), Constants.ENABLE_DELETE, Integer.class);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(delegateTask.getProcessDefinitionId()).singleResult();
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult();
        TaskDTOAdapter taskDto = new TaskDTOAdapter();
        taskDto.setSpecialUser(specialUsers);
        // assignee的权限最大
        if (StringUtils.isNotEmpty(delegateTask.getAssignee())) {
            taskDto.setCandidateUser(Collections.singleton(delegateTask.getAssignee()));
        } else {
            Set<String> candidateUsers = delegateTask.getCandidates().stream().filter(idLink -> "candidate".equals(idLink.getType())).map(IdentityLink::getUserId).collect(Collectors.toSet());
            taskDto.setCandidateUser(candidateUsers);
        }
        taskDto.setProcessName(processName);
        taskDto.setEnableDelete(enableDelete);
        taskDto.setFormNo(formNo);
        taskDto.setFormData(formData);
        taskDto.setInitiator(initiator);
        taskDto.setProcessId(delegateTask.getProcessInstanceId());
        taskDto.setProcessKey(processDefinition.getKey());
        taskDto.setVersion(Integer.parseInt(deployment.getKey()));
        taskDto.setInstanceId(delegateTask.getId());
        taskDto.setTaskName(delegateTask.getName());
        taskDto.setActivityName(delegateTask.getTaskDefinitionKey());
        taskDto.setProcessDefinitionId(delegateTask.getProcessDefinitionId());
        taskDto.setPageUrl(pageUrl);
        taskDto.setMultiple(Boolean.valueOf(multipleInstance));
        taskDto.setProtocols(protocols);
        return taskDto;
    }
    
}
