/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.supcon.supfusion.flow.common.dto.*;
import org.dom4j.DocumentException;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;

public interface BpmnService {
    
    /**
     * 获取人工节点属性值
     * @param taskDefinitionKey 人工节点ID
     * @param processInstanceId 流程实例ID
     * @param attributeKey 属性key
     * @return
     */
    String getUserTaskAttribute(String taskDefinitionKey, String processInstanceId, String attributeKey);
    
    /**
     * 批量获取
     * @param taskInstanceIds
     * @param attributeKey
     * @return
     */
    Map<String, String> batchGetUserTaskAttribute(Collection<String> taskInstanceIds, String attributeKey);

    /**
     * 判断当前环节是否为会签
     * @param processId
     * @param currentUserTaskKey
     * @return
     */
    boolean isLoopTask(String processId, String currentUserTaskKey);
    /**
     * 查询下游人工节点ID列表
     * @param processId
     * @param currentUserTaskKey
     * @return
     */
    List<ElementDTO> queryNextUserTaskKey(String processId, String currentUserTaskKey) throws DocumentException;

    /**
     * 获取人工节点属性值
     * @param taskInstanceId 待办ID
     * @param attributeKey 属性key
     * @return
     */
    String getUserTaskAttribute(String taskInstanceId, String attributeKey);
    
    /**
     * 查询当前环节下审批分支信息
     * @param taskInstanceId
     * @param processId
     * @return
     */
    List<AuditDTO> listAudit(String taskInstanceId, String processId) throws DocumentException;
    
    /**
     * 查询目标节点的指派属性
     * @param taskInstanceId
     * @param processId
     * @return
     */
    List<AssigneeDTO> listTaskAssigneeBranch(String taskInstanceId, String processId) throws DocumentException;
    /**
     * 获取启动页的指派分支
     * @param startEvent
     * @param firstUserTask
     * @param processDefinitionId
     * @param enableSave
     * @return
     * @throws DocumentException
     */
    List<AssigneeDTO> listProcessAssigneeBranch(StartEvent startEvent, UserTask firstUserTask, String processDefinitionId
            , boolean enableSave) throws DocumentException;
    /**
     * 
     * @param userTask
     * @param processDefinitionId 流程模板ID
     * @return
     */
    List<AuditDTO> listAudit(UserTask userTask, String processDefinitionId) throws DocumentException;
    
    /**
     * 获取所有人工节点的用户组
     * @param processKey 流程编码
     * @param processVersion 流程版本
     * @return
     */
    Set<CandidateGroupDTO> queryUserTaskCandidateGroup(String processKey, int processVersion);
    
    /**
     * 查询待办接收者规则
     * @param taskDefinitionKey
     * @param processDefinitionId
     * @return
     */
    List<RecipientRuleDTO> queryRecipientRules(String taskDefinitionKey, String processDefinitionId);
    /**
     * 
     * @param taskInstanceId
     * @return
     */
    List<RecipientRuleDTO> queryRecipientRules(String taskInstanceId);
    
    /**
     * 查询当前人工或自动环节上oodm配置
     * @param processId 流程实例ID
     * @param activityId 节点ID
     * @return
     */
    OodmSettingDTO getOodmSettings(String processId, String activityId);
    
    OodmSettingDTO getOodmSettingsInternal(String processDefinitionId, String activityId);
    
    /**
     * 
     * @param processInstanceId
     * @param taskInstanceId
     * @return
     */
    Set<String> getSubmitRecords(String processInstanceId, String taskInstanceId);
    
    /**
     * 驳回时判断, 被撤回的任务和当前进行中的任务之间是否有跨越并行网关
     * @param startInstanceId 进行中的任务ID
     * @param endInstanceId 被撤回任务ID
     * @return
     */
    boolean isCrossParallelGateway(String processInstanceId, String startInstanceId, String endInstanceId);
    /**
     * 
     * @param id
     * @param taskInstanceId
     * @return
     * @throws DocumentException
     */
    public AuditDTO getAudit(String id, String taskInstanceId) throws DocumentException;
    
    
    AuditDTO getAudit(String id, String processKey, int version) throws DocumentException;
}
