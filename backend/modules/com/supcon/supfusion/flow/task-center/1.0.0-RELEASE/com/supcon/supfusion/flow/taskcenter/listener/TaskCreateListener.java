/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.flow.common.exception.IllegalParameterException;
import com.supcon.supfusion.flow.common.po.*;
import com.supcon.supfusion.flow.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.dto.TaskDTOAdapter;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.enumeration.TaskSourceEnum;
import com.supcon.supfusion.flow.common.enumeration.TaskStatusEnum;
import com.supcon.supfusion.flow.common.enumeration.TaskTypeEnum;
import com.supcon.supfusion.flow.common.util.CodeGenerator;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.engine.server.listener.AbstractTaskListener;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.flow.taskcenter.job.TaskCombineJob;
import com.supcon.supfusion.flow.taskcenter.register.RecipientRuleContext;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.flow.taskcenter.service.rule.RuleService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午3:04:41
 */
@Component("taskCreateListener")
public class TaskCreateListener extends AbstractTaskListener {

    private static final long serialVersionUID = 1L;
    
    @Autowired
    private transient PendingTaskMapper taskCenterMapper;
    @Autowired
    private transient ProcessMapper processMapper;
    @Autowired
    private transient ProcessAttentionMapper attentionMapper;
    @Autowired
    private transient TaskFormMapper taskFormMapper;
    @Autowired
    private transient DiagramMapper diagramMapper;
    @Autowired
    private transient BpmnService bpmnService;
    @Autowired
    private transient UserServiceAdapter userServiceAdapter;
    @Autowired
    private transient RecipientRuleContext recipientRuleContext;
    @Autowired
    private transient TaskCombineJob taskCombineJob;
    @Autowired
    private transient RedisUtils redisUtils;
    @ServiceApiReference
    private transient PersonApiService orgService;
    /**
     * @throws DocumentException 
     * @see AbstractTaskListener#createPendingTask(com.supcon.supfusion.flow.common.dto.TaskDTOAdapter)
     */
    @Override
    public void createPendingTask(TaskDTOAdapter taskDTOAdapter) {
        Set<String> taskUsers = getTaskAssignees(taskDTOAdapter);
        // 待办执行者不能为空
        if (taskUsers.isEmpty()) {
            throw new IllegalParameterException(FlowErrorEnum.TASK_ASSIGNEE_NOT_EMPTY_ERROR);
        }
        List<PendingTaskPO> taskPOs = buildTaskPO(taskDTOAdapter, taskUsers);
        // 存放需要发送待办通知的待办ID
        List<String> taskIds = new LinkedList<>();
        for (PendingTaskPO taskPo : taskPOs) {
            taskIds.add(taskPo.getId() + "");
            taskCenterMapper.insert(taskPo);
        }
        if (taskDTOAdapter.getFormData() != null) {
            insertTaskFormPo(taskPOs, taskDTOAdapter.getFormData());
        }
        // 更新我的发起和我的关注的单据编号
        updateProcessFormNo(taskDTOAdapter);
        updateAttentionFormNo(taskDTOAdapter);

        // 发送待办接收通知, 撤回产生的待办除外
        if (LocalContext.getContext().getOperationType() != OperationTypeEnum.REVOKE) {
            String exists = redisUtils.getStringValue(taskDTOAdapter.getProcessId());
            if (exists != null) {
                String[] tidArray = exists.split(Constants.SPLIT_COMMA);
                for (String tid : tidArray) {
                    taskIds.add(tid);
                }
            }
            redisUtils.setStringValue(taskDTOAdapter.getProcessId(), String.join(Constants.SPLIT_COMMA, taskIds), 30000);
        }
        taskCombineJob.submit(taskDTOAdapter.getProcessId()); 
    }

    private void updateAttentionFormNo(TaskDTOAdapter taskDTOAdapter) {
        ProcessAttentionPO pa = new ProcessAttentionPO();
        pa.setId(Long.parseLong(taskDTOAdapter.getProcessId()));
        pa.setTableNo(taskDTOAdapter.getFormNo() == null ? "" : taskDTOAdapter.getFormNo());
        attentionMapper.updateById(pa);
    }

    private void updateProcessFormNo(TaskDTOAdapter taskDTOAdapter) {
        ProcessPO p = new ProcessPO();
        p.setId(Long.parseLong(taskDTOAdapter.getProcessId()));
        p.setTableNo(taskDTOAdapter.getFormNo() == null ? "" : taskDTOAdapter.getFormNo());
        processMapper.updateById(p);
    }
    
    private Set<String> getTaskAssignees(TaskDTOAdapter taskDTOAdapter) {
        // specialUser是指撤回,指派,驳回动作产生的特定待办执行者
        if (taskDTOAdapter.getSpecialUser() != null && !taskDTOAdapter.getSpecialUser().isEmpty()) {
            return taskDTOAdapter.getSpecialUser();
        }
        Set<String> taskUsers = taskDTOAdapter.getCandidateUser();
        // 如果是会签任务, 执行者已经由MultipleTaskStartListener过滤
        if (Boolean.valueOf(taskDTOAdapter.isMultiple())) {
            return taskUsers;
        }
        List<RecipientRuleDTO> rules = bpmnService.queryRecipientRules(taskDTOAdapter.getActivityName(), taskDTOAdapter.getProcessDefinitionId());
        // 一次性请求所有的人员信息
        List<Long> personIds = rules.stream()
                .filter(r -> r.getRecipientSelect() == RecipientSelection.PERSON)
                .map(RecipientRuleDTO::getValue)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        for (RecipientRuleDTO rule : rules) {
            RuleService<RecipientRuleDTO, Collection<PersonDetailDTO>> ruleService = recipientRuleContext.getInstance(rule.getRecipientSelect());
            if (!personIds.isEmpty()) {
                Map<Long, PersonDetailDTO> personMap = orgService.queryPersonsByIds(personIds);
                if (personMap != null && !personMap.isEmpty()) {
                    ruleService.setTemporaryPersonCache(personMap);
                }
            }
            Collection<PersonDetailDTO> persons = ruleService.parse(rule, taskDTOAdapter.getProcessId());
            Set<String> userIds = persons.stream().filter(Objects::nonNull).map(p -> p.getUserId().toString()).collect(Collectors.toSet());
            taskUsers.addAll(userIds);
        }
        return taskUsers;
    }
    
    private void insertTaskFormPo(List<PendingTaskPO> taskPos, String formData) {
        for (PendingTaskPO taskPo : taskPos) {
            TaskFormPO taskForm = new TaskFormPO();
            taskForm.setId(CodeGenerator.generateUUID());
            taskForm.setProcessId(taskPo.getProcessId());
            taskForm.setInstanceId(taskPo.getInstanceId());
            taskForm.setUserId(taskPo.getUserId());
            taskForm.setFormData(formData);
            taskForm.setCreator(UserContext.getUserContext().getUserName());
            taskForm.setCreateStaffId(UserContext.getUserContext().getStaffId());
            taskFormMapper.insert(taskForm);
        }
    }
    
    private List<PendingTaskPO> buildTaskPO(TaskDTOAdapter taskDTOAdapter, Set<String> taskUsers) {
        String tenantId = RpcContext.getContext().getTenantId();
        DiagramPO diagram = diagramMapper.selectSingle(taskDTOAdapter.getProcessKey(), taskDTOAdapter.getVersion(), tenantId);
        List<PendingTaskPO> pendings = new ArrayList<>();
        for (String taskUser : taskUsers) {
            PendingTaskPO taskPO = new PendingTaskPO();
            taskPO.setId(CodeGenerator.generateUUID());
            Long userId = Long.parseLong(taskUser);
            taskPO.setUserId(userId);
            String personName = userServiceAdapter.getPersonName(userId);
            if (taskDTOAdapter.getEnableDelete() != null 
                    && taskDTOAdapter.getEnableDelete().intValue() == Constants.ENABLED) {
                taskPO.setTaskType(TaskTypeEnum.EDIT.getType());
            }
            // 查询是否关注了当前流程
            Integer attention = attentionMapper.selectCount(new QueryWrapper<ProcessAttentionPO>().lambda()
                    .eq(ProcessAttentionPO::getProcessId, taskDTOAdapter.getProcessId())
                    .eq(ProcessAttentionPO::getUserId, taskUser));
            taskPO.setMultiCompany(diagram.getMultiCompany());
            taskPO.setPersonName(personName);
            taskPO.setAppId(diagram.getAppId());
            taskPO.setCid(diagram.getCid());
            taskPO.setProcessKey(diagram.getProcessKey());
            taskPO.setProcessVersion(diagram.getVersion());
            taskPO.setTableNo(taskDTOAdapter.getFormNo());
            if (taskDTOAdapter.getInitiator() != null) {
                String staffName = userServiceAdapter.getPersonName(Long.parseLong(taskDTOAdapter.getInitiator()));
                taskPO.setInitiatorId(taskDTOAdapter.getInitiator());
                taskPO.setStaffName(staffName);
            }
            taskPO.setAttention(attention == null || attention.intValue() == 0 ? Constants.DISABLED : Constants.ENABLED);
            taskPO.setOpenUrl(taskDTOAdapter.getPageUrl());
            taskPO.setProcessId(taskDTOAdapter.getProcessId());
            if (StringUtils.isNotEmpty(taskDTOAdapter.getProcessName())) {
                taskPO.setProcessName(taskDTOAdapter.getProcessName());
                taskPO.setProcessDescription(taskDTOAdapter.getProcessName());
            } else {
                taskPO.setProcessName(diagram.getProcessName());
                taskPO.setProcessDescription(diagram.getProcessName());
            }
            taskPO.setTaskDescription(taskDTOAdapter.getTaskName());
            taskPO.setInstanceId(taskDTOAdapter.getInstanceId());
            taskPO.setActivityName(taskDTOAdapter.getActivityName());
            taskPO.setTaskSource(TaskSourceEnum.SUPOS.getSourceName());
            taskPO.setTenantId(tenantId);
            taskPO.setTaskStatus(TaskStatusEnum.ACTIVED.getStatus());
            taskPO.setLatestUser(UserContext.getUserContext().getStaffName());
            taskPO.setCreator(UserContext.getUserContext().getUserName());
            taskPO.setCreateStaffId(UserContext.getUserContext().getStaffId());
            taskPO.setTaskDescriptionZhCn(taskDTOAdapter.getTaskName());
            pendings.add(taskPO);
        }
        return pendings;
    }

    @Override
    public void deletePendingTask(String taskInstanceId) {
        // do nothing here
    }

}
