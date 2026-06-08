/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.supcon.supfusion.flow.common.dto.*;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.DomUtils;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年5月27日 上午11:15:30
 */
@Service
@Slf4j
public class BpmnServiceImpl implements BpmnService {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    
    /**
     * {@link #listAudit(UserTask, String)}
     * @throws DocumentException 
     */
    public List<AuditDTO> listAudit(String taskInstanceId, String processInstanceId) throws DocumentException {
        Task task = taskService.createTaskQuery().taskId(taskInstanceId).processInstanceId(processInstanceId).singleResult();
        if (task == null) {
            return new ArrayList<>(1);
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        UserTask userTask = (UserTask)bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        return listAudit(userTask, task.getProcessDefinitionId());
    }
    /**
     * <ul>
     *      <li>查询当前环节的配置</li>
     *      <li>找出所有当前环节所有输出迁移线</li>
     *      <li>找出每条迁移线的目标人工环节</li>
     * </ul>
     * @param userTask 当前用户任务节点
     * @param processDefinitionId 流程定义ID
     * @return
     * @throws DocumentException 
     */
    public List<AuditDTO> listAudit(UserTask userTask, String processDefinitionId) throws DocumentException {
        InputStream processModel = repositoryService.getProcessModel(processDefinitionId);
        Document document = DomUtils.getDocument(processModel);
        List<SequenceFlow> outgoingFlows = userTask.getOutgoingFlows();
        List<AuditDTO> outputs = new ArrayList<>();
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            String reject = DomUtils.getAttributeValue(document, sequenceFlow.getId(), Constants.REJECT_SEQUENCE);
            AuditDTO outputBranch = new AuditDTO();
            outputBranch.setType(Boolean.valueOf(reject) ? Constants.REJECT : 0);
            outputBranch.setId(sequenceFlow.getId());
            outputBranch.setName(sequenceFlow.getName());
            String orderStr = DomUtils.getAttributeValue(document, sequenceFlow.getId(), Constants.SEQUENCE_ORDER);
            outputBranch.setOrder(orderStr == null ? 0 : Integer.parseInt(orderStr));
            if (sequenceFlow.getConditionExpression() != null) {
                String conditionValue = resolveConditionExpression(sequenceFlow.getConditionExpression());
                outputBranch.setValue(conditionValue);
            }
            outputBranch.setTargetDefKey(sequenceFlow.getTargetRef());
            outputs.add(outputBranch);
        }
        return outputs;
    }
    
    /**
     * 查询目标节点的指派属性
     * @param taskInstanceId
     * @param processInstanceId
     * @return
     * @throws DocumentException 
     */
    public List<AssigneeDTO> listTaskAssigneeBranch(String taskInstanceId, String processInstanceId) throws DocumentException {
        Task task = taskService.createTaskQuery().taskId(taskInstanceId).processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        UserTask userTask = (UserTask)bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        InputStream processModel = repositoryService.getProcessModel(task.getProcessDefinitionId());
        Document document = DomUtils.getDocument(processModel);
        List<SequenceFlow> outgoingFlows = userTask.getOutgoingFlows();
        List<AssigneeDTO> assigns = new LinkedList<>();
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            List<AssigneeDTO> assignees = queryNextNodeAssignInfo(document, sequenceFlow.getId(), sequenceFlow, 0);
            if (assignees != null) {
                assigns.addAll(assignees);
            }
        }
        return assigns;
    }
    
    /**
     * 获取启动页的指派分支
     * @param startEvent
     * @param firstUserTask
     * @param processDefinitionId
     * @param enableSave
     * @return
     * @throws DocumentException
     */
    public List<AssigneeDTO> listProcessAssigneeBranch(StartEvent startEvent, UserTask firstUserTask, String processDefinitionId
            , boolean enableSave) throws DocumentException {
        List<AssigneeDTO> assigns = new ArrayList<>();
        List<SequenceFlow> outgoingFlows = enableSave ? firstUserTask.getOutgoingFlows() : startEvent.getOutgoingFlows();
        InputStream processModel = repositoryService.getProcessModel(processDefinitionId);
        Document document = DomUtils.getDocument(processModel);
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            List<AssigneeDTO> assignees = queryNextNodeAssignInfo(document, sequenceFlow.getId(), sequenceFlow, 0);
            if (assignees != null) {
                assigns.addAll(assignees);
            }
        }
        return assigns;
    }

    /**
     * 判断是否为会签节点
     * @param processId
     * @param currentUserTaskKey
     * @return
     */
    public boolean isLoopTask(String processId, String currentUserTaskKey) {
        HistoricProcessInstance hp = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(hp.getProcessDefinitionId());
        Process mainProcess = bpmnModel.getMainProcess();
        UserTask userTask = (UserTask)mainProcess.getFlowElement(currentUserTaskKey, true);
        if (userTask == null) {
            throw new NotExistException(FlowErrorEnum.USERTASK_NOT_EXIST);
        }
        String multipleInstance = userTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.MULTIPLE_INSTANCE);
        return Boolean.valueOf(multipleInstance);
    }
    
    /**
     * @throws DocumentException 
     * @see BpmnService#queryNextUserTaskKey(java.lang.String, java.lang.String)
     */
    public List<ElementDTO> queryNextUserTaskKey(String processId, String currentUserTaskKey) throws DocumentException {
        HistoricProcessInstance hp = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(hp.getProcessDefinitionId());
        InputStream processModel = repositoryService.getProcessModel(hp.getProcessDefinitionId());
        Document document = DomUtils.getDocument(processModel);
        Process mainProcess = bpmnModel.getMainProcess();
        UserTask userTask = (UserTask)mainProcess.getFlowElement(currentUserTaskKey, true);
        if (userTask == null) {
            throw new NotExistException(FlowErrorEnum.USERTASK_NOT_EXIST);
        }
        List<ElementDTO> targets = new ArrayList<>();
        for (SequenceFlow outgoingFlow : userTask.getOutgoingFlows()) {
            Set<String> targetKeys = recuFindUserTaskKey(outgoingFlow, currentUserTaskKey);
            String reject = DomUtils.getAttributeValue(document, outgoingFlow.getId(), Constants.REJECT_SEQUENCE);
            for (String targetKey : targetKeys) {
                targets.add(new ElementDTO(Boolean.valueOf(reject), targetKey));
            }
        }
        return targets;
    }
    
    private Set<String> recuFindUserTaskKey(SequenceFlow outgoingFlow, String originKey) {
        FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
        Set<String> targets = new HashSet<>();
        // 避免无限递归
        if (targetFlowElement == null || originKey.equals(targetFlowElement.getId())) {
            return targets;
        }
        if (targetFlowElement instanceof UserTask) {
            targets.add(targetFlowElement.getId());
            return targets;
        }
        FlowNode targetFlowNode = (FlowNode)targetFlowElement;
        for (SequenceFlow sq : targetFlowNode.getOutgoingFlows()) {
            Set<String> targetKeys = recuFindUserTaskKey(sq, originKey);
            if (!targetKeys.isEmpty()) {
                targets.addAll(targetKeys);
            }
        }
        return targets;
    }
    
    
    /**
     * 找出当前迁移线下游所有的人工环节
     * @param originSequenceFlowId 人工环节最原始的输出迁移线ID
     * @param sequenceFlow
     * @return
     * @throws DocumentException 
     */
    private List<AssigneeDTO> queryNextNodeAssignInfo(Document document, String originSequenceFlowId, SequenceFlow sequenceFlow, int deep) 
            throws DocumentException {
        // 判断是否驳回 或者 最多递归3次
        String reject = DomUtils.getAttributeValue(document, sequenceFlow.getId(), Constants.REJECT_SEQUENCE);
        if (Boolean.valueOf(reject) || deep >= 3) {
            return null;
        }
        String reassign = DomUtils.getAttributeValue(document, sequenceFlow.getId(), Constants.ASSIGN);
        FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
        if (targetFlowElement instanceof UserTask) {
            if (Boolean.valueOf(reassign)) {
                AssigneeDTO assigneeDTO = new AssigneeDTO(originSequenceFlowId, targetFlowElement.getName(), targetFlowElement.getId(), null);
                return Collections.singletonList(assigneeDTO);
            }
            return null;
        }
        List<SequenceFlow> outgoingFlows = ((FlowNode)targetFlowElement).getOutgoingFlows();
        List<AssigneeDTO> assigns = new LinkedList<>();
        // 闭链的流程会造成死循环, deep来控制递归的深度,避免死循环
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            List<AssigneeDTO> recurrenceList = queryNextNodeAssignInfo(document, originSequenceFlowId, outgoingFlow, ++deep);
            if (recurrenceList != null) {
                assigns.addAll(recurrenceList);
            }
        }
        return assigns;
    }
    
    private String resolveConditionExpression(String conditionExpression) {
        final Pattern p = Pattern.compile(Constants.AUDIT_PATTERN);
        Matcher matcher = p.matcher(conditionExpression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "0";
    }
    
    /**
     * @see BpmnService#getUserTaskAttribute(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String getUserTaskAttribute(String taskDefinitionKey, String processInstanceId, String attributeKey) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
        if (flowElement instanceof UserTask) {
            return ((UserTask)flowElement).getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, attributeKey);
        }
        return "";
    }
    
    public Map<String, String> batchGetUserTaskAttribute(Collection<String> taskInstanceIds, String attributeKey) {
        Map<String, String> result = new HashMap<>();
        if (taskInstanceIds.isEmpty()) {
            return result;
        }
        String sql = "select ID_, PROC_DEF_ID_, TASK_DEF_KEY_ from ACT_RU_TASK where ID_ IN (%s)";
        StringBuilder ids = new StringBuilder();
        for (String taskInstanceId : taskInstanceIds) {
            ids.append(",'").append(taskInstanceId).append("'");
        }
        sql = String.format(sql, ids.substring(1));
        List<Task> tasks = taskService.createNativeTaskQuery().sql(sql).list();
        for (Task t : tasks) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(t.getProcessDefinitionId());
            FlowElement flowElement = bpmnModel.getFlowElement(t.getTaskDefinitionKey());
            String value = ((UserTask)flowElement).getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, attributeKey);
            result.put(t.getId(), value);
        }
        return result;
    }
    
    /**
     * @see BpmnService#getUserTaskAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public String getUserTaskAttribute(String taskInstanceId, String attributeKey) {
        String processDefinitionId = "";
        String taskDefinitionKey = "";
        Task activeTask = taskService.createTaskQuery().taskId(taskInstanceId).singleResult();
        if (activeTask == null) {
            HistoricTaskInstance hisTask = historyService.createHistoricTaskInstanceQuery().taskId(taskInstanceId).singleResult();
            if (hisTask == null) {
                return "";
            }
            processDefinitionId = hisTask.getProcessDefinitionId();
            taskDefinitionKey = hisTask.getTaskDefinitionKey();
        } else {
            processDefinitionId = activeTask.getProcessDefinitionId();
            taskDefinitionKey = activeTask.getTaskDefinitionKey();
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
        if (flowElement instanceof UserTask) {
            return ((UserTask)flowElement).getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, attributeKey);
        }
        return "";
    }
    
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.BpmnService#queryUserTaskCandidateGroup(java.lang.String, int)
     */
    @Override
    public Set<CandidateGroupDTO> queryUserTaskCandidateGroup(String processKey, int processVersion) {
        List<Deployment> latestDeployment = repositoryService.createDeploymentQuery()
                .deploymentKey(processVersion + "")
                .processDefinitionKey(processKey)
                .orderByDeploymenTime().desc()
                .listPage(0, 1);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(latestDeployment.get(0).getId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        List<UserTask> userTasks = bpmnModel.getMainProcess().findFlowElementsOfType(UserTask.class, true);
        Set<CandidateGroupDTO> groupSet = new HashSet<>();
        for (UserTask userTask : userTasks) {
            List<RecipientRuleDTO> recipientRules = queryRecipientRules(userTask.getId(), processDefinition.getId());
            for (RecipientRuleDTO recipientRule : recipientRules) {
                CandidateGroupDTO candidateGroup = new CandidateGroupDTO(userTask.getId(), userTask.getName());
                if (recipientRule.getRecipientSelect() == RecipientSelection.DEPART) {
                    candidateGroup.setDepart(recipientRule.getValue());
                    groupSet.add(candidateGroup);
                } else if (recipientRule.getRecipientSelect() == RecipientSelection.ROLE) {
                    candidateGroup.setRole(recipientRule.getValue());
                    groupSet.add(candidateGroup);
                } else if (recipientRule.getRecipientSelect() == RecipientSelection.POSITION) {
                    candidateGroup.setPosition(recipientRule.getValue());
                    groupSet.add(candidateGroup);
                }
            }
        }
        return groupSet;
    }
    
    /**
     * 查询待办接收者规则
     * @param taskDefinitionKey
     * @param processDefinitionId
     * @return
     */
    public List<RecipientRuleDTO> queryRecipientRules(String taskDefinitionKey, String processDefinitionId) {
        List<RecipientRuleDTO> recipientRules = new LinkedList<>();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        UserTask userTask = (UserTask)bpmnModel.getFlowElement(taskDefinitionKey);
        Map<String, List<ExtensionElement>> extensionElementMap = userTask.getExtensionElements();
        for (Map.Entry<String, List<ExtensionElement>> extensionElements : extensionElementMap.entrySet()) {
            for (ExtensionElement extensionElement : extensionElements.getValue()) {
                if (Constants.TAG_ASSIGNEERULE.equals(extensionElement.getName())) {
                    recipientRules.add(buildRecipientRule(extensionElement));
                }
            }
        }
        return recipientRules;
    }
    
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.BpmnService#getOodmSettings(java.lang.String, java.lang.String)
     */
    public OodmSettingDTO getOodmSettings(String processId, String activityId) {
        List<HistoricActivityInstance> hais = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processId)
                .activityId(activityId)
                .orderByHistoricActivityInstanceStartTime().desc()
                .listPage(0, 1);
        if (hais.isEmpty()) {
            log.error("获取oodm配置失败, 当前节点的提交记录不存在, activityId={}", activityId);
            return null;
        }
        return getOodmSettingsInternal(hais.get(0).getProcessDefinitionId(), activityId);
    }
    
    public OodmSettingDTO getOodmSettingsInternal(String processDefinitionId, String activityId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowElement flowElement = bpmnModel.getFlowElement(activityId);
        Map<String, List<ExtensionElement>> extensionElementMap = flowElement.getExtensionElements();
        for (Map.Entry<String, List<ExtensionElement>> extensionElements : extensionElementMap.entrySet()) {
            for (ExtensionElement extensionElement : extensionElements.getValue()) {
                if (Constants.TAG_OODM.equals(extensionElement.getName())) {
                    return buildOodmSetting(extensionElement);
                }
            }
        }
        return null;
    }
    
    private OodmSettingDTO buildOodmSetting(ExtensionElement extensionElement) {
        Map<String, List<ExtensionAttribute>> attributes = extensionElement.getAttributes();
        String templateNamespace = attributes.get(Constants.OODM_TEMPLATE_NAMESPACE).get(0).getValue();
        String templateName = attributes.get(Constants.OODM_TEMPLATE_NAME).get(0).getValue();
        String instanceName = attributes.get(Constants.OODM_INSTANCE_NAME).get(0).getValue();
        String serviceNamespace = attributes.get(Constants.OODM_SERVICE_NAMESPACE).get(0).getValue();
        String serviceName = attributes.get(Constants.OODM_SERVICE_NAME).get(0).getValue();
        return new OodmSettingDTO(templateNamespace, templateName, instanceName, serviceNamespace, serviceName);
    }
    
    private RecipientRuleDTO buildRecipientRule(ExtensionElement extensionElement) {
        Map<String, List<ExtensionAttribute>> attributes = extensionElement.getAttributes();
        String name = attributes.get(Constants.NAME).get(0).getValue();
        String value = attributes.get(Constants.VALUE).get(0).getValue();
        String posRestrict = attributes.get(Constants.POSITION_RESTRICT).get(0).getValue();
        String groupRestrict = attributes.get(Constants.GROUP_RESTRICT).get(0).getValue();
        String positions = attributes.get(Constants.POSITION).get(0).getValue();
        String persons = attributes.get(Constants.PERSON).get(0).getValue();
        String unrestrict = attributes.get(Constants.UNRESTRICT).get(0).getValue();
        RecipientRuleDTO recipientRule = new RecipientRuleDTO();
        RecipientSelection recipient = RecipientSelection.getByName(name);
        recipientRule.setRecipientSelect(recipient);
        recipientRule.setGroupRestrict(Boolean.valueOf(groupRestrict));
        if (StringUtils.isNotEmpty(value)) {
            recipientRule.setValue(value);
        }
        if (StringUtils.isNotEmpty(persons)) {
            recipientRule.setPersons(Arrays.asList(persons.split(Constants.SPLIT_COMMA)));
        }
        if (StringUtils.isNotEmpty(positions)) {
            recipientRule.setPositions(Arrays.asList(positions.split(Constants.SPLIT_COMMA)));
        }
        recipientRule.setPosRestrict(Boolean.valueOf(posRestrict));
        recipientRule.setUnrestrict(Boolean.valueOf(unrestrict));
        return recipientRule;
    }
    
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.BpmnService#queryRecipientRules(java.lang.String)
     */
    @Override
    public List<RecipientRuleDTO> queryRecipientRules(String taskInstanceId) {
        HistoricTaskInstance hisTask = historyService.createHistoricTaskInstanceQuery().taskId(taskInstanceId).singleResult();
        return queryRecipientRules(hisTask.getTaskDefinitionKey(), hisTask.getProcessDefinitionId());
    }
    
    /**
     * 获取某个时间点之后的提交记录
     * @param processInstanceId
     * @param finishedTaskInstanceId
     * @return
     */
    @Override
    public Set<String> getSubmitRecords(String processInstanceId, String finishedTaskInstanceId) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).taskId(finishedTaskInstanceId).singleResult();
        List<HistoricActivityInstance> afterRecords = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType(Constants.ACTIVITY_TYPE_USERTASK)
                .startedAfter(task.getEndTime())
                .list();
        return afterRecords.stream().map(HistoricActivityInstance::getTaskId).collect(Collectors.toSet());
    }
    
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.BpmnService#isCrossParallelGateway(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean isCrossParallelGateway(String processInstanceId, String startInstanceId, String endInstanceId) {
        HistoricTaskInstance startTask = historyService.createHistoricTaskInstanceQuery().taskId(startInstanceId).singleResult();
        HistoricTaskInstance endTask = historyService.createHistoricTaskInstanceQuery().taskId(endInstanceId).singleResult();
        if (startTask == null || endTask == null) {
            return false;
        }
        log.error("开始时间: {}, 结束时间: {}", startTask.getEndTime(), endTask.getCreateTime());
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .startedAfter(startTask.getEndTime())
                .startedBefore(endTask.getCreateTime())
                .activityType(Constants.ACTIVITY_TYPE_PARALLELGATEWAY)
                .count();
        return count > 0;
    }
    
    public AuditDTO getAudit(String id, String taskInstanceId) throws DocumentException {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskId(taskInstanceId).singleResult();
        return getAuditInternal(id, task.getProcessDefinitionId());
    }
    
    private AuditDTO getAuditInternal(String id, String processDefitionId) throws DocumentException {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefitionId);
        FlowElement sequence = bpmnModel.getMainProcess().getFlowElement(id);
        if (sequence == null) {
            return null;
        }
        InputStream processModel = repositoryService.getProcessModel(processDefitionId);
        Document document = DomUtils.getDocument(processModel);
        String name = DomUtils.getAttributeValue(document, id, "name");
        String order = DomUtils.getAttributeValue(document, id, "order");
        String reject = DomUtils.getAttributeValue(document, id, "rejectToSubmitter");
        AuditDTO audit = new AuditDTO();
        audit.setId(id);
        audit.setName(name);
        audit.setOrder(Integer.parseInt(order));
        audit.setTargetDefKey(((SequenceFlow)sequence).getTargetRef());
        audit.setType(Boolean.valueOf(reject) ? Constants.REJECT : 0);
        return audit;
    }
    
    /**
     * @throws DocumentException 
     * @see com.supcon.supfusion.flow.engine.server.service.BpmnService#getAudit(java.lang.String, java.lang.String, int)
     */
    @Override
    public AuditDTO getAudit(String id, String processKey, int version) throws DocumentException {
        List<Deployment> latestDeployment = repositoryService.createDeploymentQuery()
                .deploymentKey(version + "")
                .processDefinitionKey(processKey)
                .orderByDeploymenTime().desc()
                .listPage(0, 1);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(latestDeployment.get(0).getId()).singleResult();
        return getAuditInternal(id, processDefinition.getId());
    }
}
