/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.DocumentException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cmd.SetProcessDefinitionVersionCmd;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.dto.AssigneeDTO;
import com.supcon.supfusion.flow.common.dto.AuditDTO;
import com.supcon.supfusion.flow.common.dto.ProcessStartDTO;
import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.exception.ProcessOperateException;
import com.supcon.supfusion.flow.common.exception.StatusAbnormalException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月25日 上午9:33:56
 */
@Service
@Slf4j
public class ProcessEngineServiceImpl implements ProcessEngineService {

    @Autowired
    private ManagementService managementService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private BpmnService bpmnService;
    
    /**
     * @throws DocumentException 
     * @see BpmnService#getProcessStartupInfo(java.lang.String)
     */
    @Override
    public ProcessStartDTO getProcessStartInfo(String processKey, int processVersion, String tenantId, boolean detail) throws DocumentException {
        DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery()
                .deploymentKey(processVersion + "")
                .processDefinitionKey(processKey)
                .orderByDeploymenTime();
        if (StringUtils.isNotEmpty(tenantId)) {
            deploymentQuery.deploymentTenantId(tenantId);
        }
        List<Deployment> latestDeployment = deploymentQuery.desc().listPage(0, 1);
        if (latestDeployment.isEmpty()) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_PUBLISHED_ERROR);
        }
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(latestDeployment.get(0).getId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process mainProcess = bpmnModel.getMainProcess();
        // 发布前已校验主流程有且只有一个开始节点
        StartEvent startEvent = mainProcess.findFlowElementsOfType(StartEvent.class, false).get(0);
//        String pageUrl = DomUtils.getAttributeValue(document, startEvent.getId(), Constants.PAGE_URL);
        UserTask firstUserTask = recursiveLookupFirstUserTask(startEvent.getOutgoingFlows());
        if (firstUserTask == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_STARTUP_USERTASK_NOT_EXIST);
        }
        ProcessStartDTO processStartInfoDTO = createProcessStartInfoDTO(firstUserTask, processDefinition.getName(), processDefinition.getId());
        if (detail) {
            List<AuditDTO> audits = bpmnService.listAudit(firstUserTask, processDefinition.getId());
            List<AssigneeDTO> assigns = bpmnService.listProcessAssigneeBranch(startEvent, firstUserTask, processDefinition.getId(), processStartInfoDTO.isEnableSave());
            processStartInfoDTO.setAudits(audits);
            processStartInfoDTO.setAssigns(assigns);
        }
        return processStartInfoDTO;
    }
    
    private ProcessStartDTO createProcessStartInfoDTO(UserTask firstUserTask, String processName, String processDefinitionId) {
        ProcessStartDTO startupDto = new ProcessStartDTO();
        startupDto.setProcessName(processName);
        startupDto.setStartTaskName(firstUserTask.getName());
        startupDto.setUrl(firstUserTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.PAGE_URL));
        List<RecipientRuleDTO> rules = bpmnService.queryRecipientRules(firstUserTask.getId(), processDefinitionId);
        if (rules.size() == 1) {
            boolean enableSave = rules.get(0).getRecipientSelect() == RecipientSelection.INITIATOR;
            startupDto.setEnableSave(enableSave);
        }
        String readonly = firstUserTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.READONLY);
        startupDto.setReadOnly(Boolean.valueOf(readonly));
        String enableComment = firstUserTask.getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.ENABLE_COMMENT);
        startupDto.setEnableComment(Boolean.valueOf(enableComment));
        return startupDto;
    }
    
    
    private UserTask recursiveLookupFirstUserTask(List<SequenceFlow> outgoingFlows) {
        if (outgoingFlows.isEmpty()) {
            return null;
        }
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
            if (targetFlowElement instanceof UserTask) {
                return (UserTask)targetFlowElement;
            }
            UserTask firstUserTask = recursiveLookupFirstUserTask(((FlowNode)targetFlowElement).getOutgoingFlows());
            if (firstUserTask != null) {
                return firstUserTask;
            }
        }
        return null;
    }
    
    /**
     * @see ProcessEngineService#migrateProcessInstanceWithTenant(java.lang.String, java.lang.String, int, java.lang.String)
     */
    @Override
    public void migrateProcessInstanceWithTenant(String oldProcessDefinitionId, int newProcessDefinitionVersion, String tenantId) {
        List<ProcessInstance> oldProcessInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionId(oldProcessDefinitionId)
                .processInstanceTenantId(tenantId)
                .list();
        for (ProcessInstance oldProcessInstance : oldProcessInstances) {
            // 将流程实例迁移到新的流程模板
            managementService.executeCommand(new SetProcessDefinitionVersionCmd(oldProcessInstance.getId(), newProcessDefinitionVersion));
        }
    }
    /**
     * @see ProcessEngineService#batchDeleteProcessInstancesWithTenant(java.lang.String, java.lang.String)
     */
    @Override
    public void batchDeleteProcessInstances(List<String> activeProcessIds, List<String> completeProcessIds) {
        for (String processId : activeProcessIds) {
            try {
                runtimeService.deleteProcessInstance(processId, "useless delete");
            } catch (Exception e) {
                log.error("删除进行中流程失败, processId={}", processId, e);
            }
        }
        for (String processId : completeProcessIds) {
            try {
                historyService.deleteHistoricProcessInstance(processId);
            } catch (Exception e) {
                log.error("删除已完成的流程失败, processId={}", processId, e);
            }
        }
    }
    /**
     * @see ProcessEngineService#startUp(java.lang.String, int, java.lang.String, java.util.Map)
     */
    @Override
    public String startUp(String processKey, int processVersion, String starter, String tenantId, Map<String, Object> processVariables, Map<String, Object> transientVariables) {
        List<Deployment> latestDeployment = repositoryService.createDeploymentQuery()
                .deploymentKey(processVersion + "")
                .processDefinitionKey(processKey)
                .deploymentTenantId(tenantId)
                .orderByDeploymenTime().desc()
                .listPage(0, 1);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(latestDeployment.get(0).getId()).singleResult();
        ProcessInstance processInstance = null;
        try {
            // 设置启动者
            identityService.setAuthenticatedUserId(starter);
            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionId(processDefinition.getId())
                    .transientVariables(transientVariables) // 变量不持久化
                    .variables(processVariables)
                    .tenantId(tenantId)
                    .start();
            return processInstance.getId();
        } catch (Exception e) {
            // ignore
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof BizException) {
                throw (BizException)rootCause;
            }
            throw new ProcessOperateException(FlowErrorEnum.PROCESS_STARTUP_FAIL, e);
        } finally {
            // 释放启动者
            identityService.setAuthenticatedUserId(null);
            if (processInstance != null) {
                Set<String> trashyKeys = transientVariables.keySet().stream()
                        .filter(k -> (!Constants.FORM_DATA.equals(k) && !Constants.FORM_NO.equals(k)))
                        .collect(Collectors.toSet());
                runtimeService.removeVariables(processInstance.getId(), trashyKeys);
            }
        }
    }
    
    /**
     * @see ProcessEngineService#suspendProcess(java.lang.String)
     */
    @Override
    public void suspendProcess(String processId) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processId).suspended().count();
        if (count > 0) {
            throw new StatusAbnormalException(FlowErrorEnum.PROCESS_SUSPENDED_ERROR);
        }
        runtimeService.suspendProcessInstanceById(processId);
    }
    
    /**
     * @see ProcessEngineService#activeProcess(java.lang.String)
     */
    @Override
    public void activeProcess(String processId) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processId).active().count();
        if (count > 0) {
            throw new StatusAbnormalException(FlowErrorEnum.PROCESS_ACTIVED_ERROR);
        }
        runtimeService.activateProcessInstanceById(processId);
    }
    
    /**
     * @see ProcessEngineService#terminateProcess(java.lang.String)
     */
    @Override
    public void terminateProcess(String processId) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processId).count();
        if (count == 0) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        runtimeService.deleteProcessInstance(processId, "process terminate");
    }
    
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.ProcessEngineService#getProcessStatus(java.lang.String)
     */
    @Override
    public ProcessStatusEnum getProcessStatus(String processId) {
        long suspendCount = runtimeService.createProcessInstanceQuery().processInstanceId(processId).suspended().count();
        return suspendCount > 0 ? ProcessStatusEnum.SUSPENDED : ProcessStatusEnum.ACTIVED;
    }

    /**
     * @see com.supcon.supfusion.flow.engine.server.service.ProcessEngineService#getInitiator(java.lang.String)
     */
    @Override
    public String getInitiator(String processId) {
        return runtimeService.getVariable(processId, Constants.PROCESS_START, String.class);
    }

    /**
     * @see com.supcon.supfusion.flow.engine.server.service.ProcessEngineService#getVariableValue(java.lang.String, java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getVariableValue(String processId, String key, Class<T> clazz) {
        try {
            return runtimeService.getVariable(processId, key, clazz);
        } catch (Exception ignore) {
            
        }
        return null;
    }

    /**
     * @see com.supcon.supfusion.flow.engine.server.service.ProcessEngineService#setVariable(java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public <T> void setVariable(String processId, String key, T value) {
        runtimeService.setVariable(processId, key, value);
    }

}
