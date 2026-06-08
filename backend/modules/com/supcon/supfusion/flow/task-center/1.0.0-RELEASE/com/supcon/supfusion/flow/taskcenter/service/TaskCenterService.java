
package com.supcon.supfusion.flow.taskcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.dto.*;
import com.supcon.supfusion.flow.common.enumeration.*;
import com.supcon.supfusion.flow.common.exception.*;
import com.supcon.supfusion.flow.common.po.*;
import com.supcon.supfusion.flow.common.util.*;
import com.supcon.supfusion.flow.common.vo.webapi.*;
import com.supcon.supfusion.flow.dao.CompleteTaskMapper;
import com.supcon.supfusion.flow.dao.EntrustMapper;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.flow.dao.TaskFormMapper;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.engine.server.service.TaskEngineService;
import com.supcon.supfusion.flow.taskcenter.job.LoggerJob;
import com.supcon.supfusion.flow.taskcenter.mybatis.TaskQueryWrapper;
import com.supcon.supfusion.flow.taskcenter.rpc.NotificationService;
import com.supcon.supfusion.flow.taskcenter.rpc.OodmServiceAdapter;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午3:49:56
 */
@Service
@Slf4j
public class TaskCenterService {

    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    @Autowired
    private ProcessService processService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TaskFormMapper taskFormMapper;
    @Autowired
    private CompleteTaskMapper completeTaskMapper;
    @Autowired
    private EntrustMapper entrustMapper;
    @Autowired
    private TaskEngineService taskEngineService;
    @Autowired
    private OodmServiceAdapter oodmServiceAdapter;
    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    private LoggerJob loggerJob;
    @Autowired
    private MessageResourceWrapper messageResourceWrapper;

    /**
     * 查询当前登录用户待办列表
     *
     * @param
     * @return
     */
    public PageResult<PendingTaskResponseVO> queryMyPendingTask(PendingQueryContractDTO queryContract, Pagination pagination) {
        Long userId = UserContext.getUserContext().getUserId();
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<PendingTaskPO> taskListQueryWrapper = TaskQueryWrapper.buildTaskListQueryWrapper(queryContract, tenantId);
        taskListQueryWrapper.eq(PendingTaskPO::getUserId, userId);
        Integer total = pendingTaskMapper.selectCount(taskListQueryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        taskListQueryWrapper.orderByDesc(PendingTaskPO::getCreateTime);
        Page<PendingTaskPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<PendingTaskPO> tasks = pendingTaskMapper.selectPage(page, taskListQueryWrapper);
        if (tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        List<PendingTaskResponseVO> taskVos = voListTransfer(tasks.getRecords());
        return new PageResult<>(taskVos, total, pagination.getPageSize(), pagination.getCurrent());
    }

    private List<PendingTaskResponseVO> voListTransfer(List<PendingTaskPO> taskPos) {
        List<PendingTaskResponseVO> voList = new LinkedList<>();
        Set<String> instanceIds = taskPos.stream().map(PendingTaskPO::getInstanceId).collect(Collectors.toSet());
        Map<String, String> showlogMap = bpmnService.batchGetUserTaskAttribute(instanceIds, Constants.ENABLE_SHOWLOG);
        for (PendingTaskPO taskPo : taskPos) {
            String processName = messageResourceWrapper.getMessageNotBlank(taskPo.getProcessDescription());
            String taskName = messageResourceWrapper.getMessageNotBlank(taskPo.getTaskDescription());
           com.supcon.supfusion.flow.common.vo.webapi.PendingTaskResponseVO.Builder builder = new PendingTaskResponseVO.Builder()
                    .setCompanyId(taskPo.getCid().toString())
                    .setAppId(taskPo.getAppId())
                    .setProcessName(processName)
                    .setFormNo(taskPo.getTableNo())
                    .setInitiator(taskPo.getStaffName())
                    .setSource(taskPo.getTaskSource())
                    .setStartTime(taskPo.getStartTime())
                    .setStatus(taskPo.getTaskStatus())
                    .setTaskId(taskPo.getId().toString())
                    .setTaskName(taskName)
                    .setType(taskPo.getTaskType())
                    .setProcessId(taskPo.getProcessId())
                    .setAttention(taskPo.getAttention() != null && taskPo.getAttention().intValue() == Constants.ENABLED)
                    .setEnableDelete(taskPo.getTaskType() != null && taskPo.getTaskType().intValue() == TaskTypeEnum.EDIT.getType())
                    .setShowlog(Boolean.valueOf(showlogMap.get(taskPo.getInstanceId())))
                    .setCheckStatus(taskPo.getHasRead() != null && taskPo.getHasRead().intValue() == Constants.ENABLED);
           if (!TaskSourceEnum.SUPOS.getSourceName().equals(taskPo.getTaskSource())) {
               builder.setUrl(taskPo.getOpenUrl());
           }
           TaskFormPO formData = taskFormMapper.selectOne(new QueryWrapper<TaskFormPO>().lambda()
                       .eq(TaskFormPO::getInstanceId, taskPo.getInstanceId())
                       .eq(TaskFormPO::getUserId, taskPo.getUserId()));
           if (formData != null) {
               builder.setFormData(formData.getFormData());
           }
           voList.add(builder.build());
        }
        return voList;
    }

    /**
     * 查询所有待办
     * @param queryContract
     * @param pagination
     * @return
     */
    public PageResult<PendingTaskResponseVO2> queryAllPendingTask(PendingQueryContractDTO queryContract, Pagination pagination) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<PendingTaskPO> taskListQueryWrapper = TaskQueryWrapper.buildTaskListQueryWrapper(queryContract, tenantId);
        taskListQueryWrapper.and(i -> i.eq(PendingTaskPO::getTaskSource, TaskSourceEnum.SUPOS.getSourceName()));
        Integer total = pendingTaskMapper.selectCount(taskListQueryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        taskListQueryWrapper.orderByDesc(PendingTaskPO::getCreateTime);
        Page<PendingTaskPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<PendingTaskPO> tasks = pendingTaskMapper.selectPage(page, taskListQueryWrapper);
        if (tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        List<PendingTaskResponseVO2> taskVos = buildTaskListVO(tasks.getRecords());
        return new PageResult<>(taskVos, total, pagination.getPageSize(), pagination.getCurrent());
    }
    
    private List<PendingTaskResponseVO2> buildTaskListVO(List<PendingTaskPO> taskPos) {
        List<PendingTaskResponseVO2> voList = new LinkedList<>();
        for (PendingTaskPO taskPo : taskPos) {
            PendingTaskResponseVO2 taskVo = new PendingTaskResponseVO2.Builder()
                    .setProcessName(taskPo.getProcessName())
                    .setProcessId(taskPo.getProcessId())
                    .setFormNo(taskPo.getTableNo())
                    .setInitiator(taskPo.getStaffName())
                    .setStartTime(taskPo.getStartTime())
                    .setStatus(taskPo.getTaskStatus())
                    .setTaskId(taskPo.getId().toString())
                    .setTaskName(taskPo.getTaskDescriptionZhCn())
                    .setAssignee(taskPo.getPersonName())
                    .setVersion(taskPo.getProcessVersion())
                    .build();
            voList.add(taskVo);
        }
        return voList;
    }
    
    /**
     * 查询已完成的任务列表
     *
     * @param queryContract
     * @param pagination
     * @return
     */
    public PageResult<CompletedTaskResponseVO> queryCompleteTask(CompleteTaskQueryContractDTO queryContract, Pagination pagination) {
        String tenantId = RpcContext.getContext().getTenantId();
        Long userId = UserContext.getUserContext().getUserId();
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = TaskQueryWrapper.buildCompleteTaskListQueryWrapper(queryContract, tenantId);
        queryWrapper.eq(CompleteTaskPO::getUserId, userId);
        Integer total = completeTaskMapper.selectCount(queryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pagination.getPageSize(), pagination.getCurrent());
        }
        queryWrapper.orderByDesc(CompleteTaskPO::getCreateTime);
        Page<CompleteTaskPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<CompleteTaskPO> pageResult = completeTaskMapper.selectPage(page, queryWrapper);
        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        List<CompletedTaskResponseVO> taskVos = completeTaskTransfer(pageResult.getRecords());
        return new PageResult<>(taskVos, total, pagination.getPageSize(), pagination.getCurrent());
    }

    private List<CompletedTaskResponseVO> completeTaskTransfer(List<CompleteTaskPO> taskPos) {
        List<CompletedTaskResponseVO> voList = new LinkedList<>();
        for (CompleteTaskPO completeTask : taskPos) {
            TaskFormPO formData = taskFormMapper.selectOne(new QueryWrapper<TaskFormPO>().lambda()
                    .eq(TaskFormPO::getInstanceId, completeTask.getInstanceId())
                    .eq(TaskFormPO::getUserId, completeTask.getUserId()));
            CompletedTaskResponseVO taskVo = new CompletedTaskResponseVO.Builder()
                    .setProcessName(completeTask.getProcessName())
                    .setFormNo(completeTask.getTableNo())
                    .setSource(completeTask.getTaskSource())
                    .setStartTime(completeTask.getStartTime())
                    .setCompleteTime(completeTask.getEndTime())
                    .setTaskId(completeTask.getId().toString())
                    .setTaskName(completeTask.getTaskName())
                    .setProcessId(completeTask.getProcessId())
                    .setInitiator(completeTask.getStaffName())
                    .setReject(completeTask.getReject() == Constants.ENABLED)
                    .setFormData(formData == null ? null : formData.getFormData())
                    .setShowlog(completeTask.getShowlog() == null ? Boolean.FALSE : (completeTask.getShowlog().intValue() == Constants.ENABLED))
                    .build();
            voList.add(taskVo);
        }
        return voList;
    }

    /**
     * 根据流程查询待办任务
     *
     * @param processId
     * @return
     */
    public List<PendingTaskPO> listPendingTaskByProcess(String processId, boolean needForm) {
        List<PendingTaskPO> tasks = pendingTaskMapper.selectList(new QueryWrapper<PendingTaskPO>().lambda()
                .eq(PendingTaskPO::getProcessId, processId));
        if (needForm) {
            for (PendingTaskPO task : tasks) {
                LambdaQueryWrapper<TaskFormPO> queryWraper = new QueryWrapper<TaskFormPO>().lambda()
                        .eq(TaskFormPO::getUserId, task.getUserId())
                        .eq(TaskFormPO::getInstanceId, task.getInstanceId());
                TaskFormPO form = taskFormMapper.selectOne(queryWraper);
                if (form != null) {
                    task.setFormData(form.getFormData());
                    task.setFormTempData(form.getFormTempData());
                }
            }
        }
        return tasks;
    }
    
    /**
     * 查询待办实例列表
     * @param processIds
     * @return
     */
    public Map<String, List<PendingTaskPO>> listTaskByProcess(List<String> processIds) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<PendingTaskPO>().lambda()
                .in(PendingTaskPO::getProcessId, processIds);
        List<PendingTaskPO> tasks = pendingTaskMapper.selectList(queryWrapper);
        return tasks.stream().collect(Collectors.groupingBy(PendingTaskPO::getProcessId));
    }
    /**
     * 查询流程进度
     * @param processId
     * @return
     */
    public List<PendingTaskPO> getProcessProgress(String processId) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<PendingTaskPO>()
                .select("distinct instance_id, task_description_zh_cn")
                .lambda()
                .eq(PendingTaskPO::getProcessId, processId);
        return pendingTaskMapper.selectList(queryWrapper);
    }
    
    /**
     * 查询该流程已经提交的待办任务
     * @param processId
     * @return
     */
    public List<CompleteTaskPO> listCompletedTaskByProcess(String processId) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = new QueryWrapper<CompleteTaskPO>().lambda()
                .eq(CompleteTaskPO::getProcessId, processId);
        return completeTaskMapper.selectList(queryWrapper);
    }
    
    /**
     * @param processId
     */
    public void updateByProcess(String processId, PendingTaskPO task) {
        LambdaQueryWrapper<PendingTaskPO> updateWrapper = Wrappers.<PendingTaskPO>lambdaQuery();
        updateWrapper.eq(PendingTaskPO::getProcessId, processId);
        pendingTaskMapper.update(task, updateWrapper);
    }

    /**
     * 当第一个人工环节执行者为发起人, 系统需要自动提交该环节待办
     *
     * @param tasks
     * @return
     */
    public boolean detectAutoSubmit(List<PendingTaskPO> tasks) {
        if (tasks.size() != 1) {
            return false;
        }
        PendingTaskPO autoCommitTask = tasks.get(0);
        return taskEngineService.detectAutoComplete(autoCommitTask.getInstanceId());
    }

    /**
     * 查询当前登录用户的待办总数
     *
     * @return 待办总数
     */
    public int queryTotals(Long userId) {
        if (userId == null) {
            return 0;
        }
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery().eq(PendingTaskPO::getUserId, userId);
        return pendingTaskMapper.selectCount(queryWrapper);
    }
    /**
     * 查询当前登录用户指定时间的待办总数
     *
     * @return 待办总数
     */
    public int queryTotalsByTime(Date startTime, Date endTime) {
        Long userId = UserContext.getUserContext().getUserId();
        if (userId == null) {
            return 0;
        }
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery().eq(PendingTaskPO::getUserId, userId)
                .ge(startTime != null, BaseEntity::getCreateTime, startTime)
                .le(endTime != null, BaseEntity::getCreateTime, endTime);
        return pendingTaskMapper.selectCount(queryWrapper);
    }

    public PendingTaskPO getById(Long taskId) {
        return pendingTaskMapper.selectById(taskId);
    }
    
    /**
     * 获取待办详情
     *
     * @param userId 用户ID
     * @param taskId 待办任务ID
     * @return
     * @throws DocumentException 
     */
    public PendingTaskResponseVO getPendingTask(Long userId, Long taskId) throws DocumentException {
        PendingTaskPO pendingTask = pendingTaskMapper.selectOne(new QueryWrapper<PendingTaskPO>().lambda().eq(PendingTaskPO::getId, taskId));
        if (pendingTask == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        if (pendingTask.getUserId().longValue() != userId.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_NO_CHECK_PERMISSION_ERROR);
        }
        // 获取输出分支信息
        List<AuditDTO> audits = bpmnService.listAudit(pendingTask.getInstanceId(), pendingTask.getProcessId());
        Iterator<AuditDTO> iterator = audits.iterator();
        while(iterator.hasNext()) {
            AuditDTO next = iterator.next();
            // 不能驳回到未走过的节点
            if (next.getType() == Constants.REJECT) {
                LambdaQueryWrapper<CompleteTaskPO> queryWrapper = new QueryWrapper<CompleteTaskPO>().lambda()
                        .eq(CompleteTaskPO::getProcessId, pendingTask.getProcessId())
                        .eq(CompleteTaskPO::getActivityName, next.getTargetDefKey());
                Page<CompleteTaskPO> footmarks = completeTaskMapper.selectPage(new Page<>(1, 1), queryWrapper);
                if (footmarks.getTotal() == 0) {
                    iterator.remove();
                }
            }
        }
        List<AssigneeDTO> assigns = bpmnService.listTaskAssigneeBranch(pendingTask.getInstanceId(), pendingTask.getProcessId());
        PendingTaskResponseVO taskResponse = taskVOTransfer(pendingTask, audits, assigns);
        // 更新待办为已读
        if (pendingTask.getHasRead() == null || pendingTask.getHasRead().intValue() == Constants.DISABLED) {
            PendingTaskPO updateTask = new PendingTaskPO();
            updateTask.setId(taskId);
            updateTask.setHasRead(Constants.ENABLED);
            pendingTaskMapper.updateById(updateTask);
        }
        return taskResponse;
    }

    /**
     * 获取任务实例,区别于 {@link #getPendingTask(Long, Long)}获取待办任务, 一个任务实例可能存在多个待办
     *
     * @param instanceId 待办任务ID
     * @return
     */
    public PendingTaskPO getTaskInstance(String instanceId) {
        List<PendingTaskPO> taskInstances = pendingTaskMapper.getTaskInstances(instanceId);
        if (taskInstances.isEmpty()) {
            return null;
        }
        return taskInstances.get(0);
    }

    /**
     * 激活某一个流程待办
     * @param processId
     */
    public void activeTask(String processId) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getProcessId, processId);
        if (tenantId != null) {
            queryWrapper.eq(PendingTaskPO::getTenantId, tenantId);
        }
        PendingTaskPO taskPo = new PendingTaskPO();
        taskPo.setTaskStatus(ProcessStatusEnum.ACTIVED.getStatus());
        pendingTaskMapper.update(taskPo, queryWrapper);
    }
    
    /**
     * 暂停某一个流程待办
     * @param processId
     */
    public void suspendTask(String processId) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getProcessId, processId);
        if (tenantId != null) {
            queryWrapper.eq(PendingTaskPO::getTenantId, tenantId);
        }
        PendingTaskPO taskPo = new PendingTaskPO();
        taskPo.setTaskStatus(ProcessStatusEnum.SUSPENDED.getStatus());
        pendingTaskMapper.update(taskPo, queryWrapper);
    }
    
    private PendingTaskResponseVO taskVOTransfer(PendingTaskPO taskPo, List<AuditDTO> audits, List<AssigneeDTO> assigns) {
        List<AuditVO> auditBranchs = new LinkedList<>();
        for (AuditDTO audit : audits) {
            AuditVO auditVo = new AuditVO();
            BeanUtils.copyProperties(audit, auditVo);
            auditBranchs.add(auditVo);
        }
        List<AssigneeVO> assignBranchs = new LinkedList<>();
        for (AssigneeDTO assign : assigns) {
            assignBranchs.add(new AssigneeVO(assign.getId(), assign.getName(), assign.getTaskDefKey()));
        }
        String addInstance = bpmnService.getUserTaskAttribute(taskPo.getInstanceId(), Constants.ENABLE_ADDINSTANCE);
        String enableComment = bpmnService.getUserTaskAttribute(taskPo.getInstanceId(), Constants.ENABLE_COMMENT);
        String readonly = bpmnService.getUserTaskAttribute(taskPo.getInstanceId(), Constants.READONLY);
        String showlog = bpmnService.getUserTaskAttribute(taskPo.getInstanceId(), Constants.ENABLE_SHOWLOG);
        LambdaQueryWrapper<TaskFormPO> queryWraper = new QueryWrapper<TaskFormPO>().lambda()
                .eq(TaskFormPO::getUserId, taskPo.getUserId())
                .eq(TaskFormPO::getInstanceId, taskPo.getInstanceId());
        TaskFormPO form = taskFormMapper.selectOne(queryWraper);
        return new PendingTaskResponseVO.Builder()
                .setCompanyId(taskPo.getCid().toString())
                .setFormData(form == null ? null : form.getFormData())
                .setFormTempData(form == null ? null : form.getFormTempData())
                .setUrl(taskPo.getOpenUrl())
                .setProcessName(taskPo.getProcessName())
                .setFormNo(taskPo.getTableNo())
                .setSource(taskPo.getTaskSource())
                .setStartTime(taskPo.getStartTime())
                .setStatus(taskPo.getTaskStatus())
                .setTaskId(taskPo.getId().toString())
                .setTaskName(taskPo.getTaskDescriptionZhCn())
                .setProcessId(taskPo.getProcessId())
                .setInitiator(taskPo.getStaffName())
                .setAddInstance(Boolean.valueOf(addInstance))
                .setEnableComment(Boolean.valueOf(enableComment))
                .setShowlog(Boolean.valueOf(showlog))
                .setReadonly(Boolean.valueOf(readonly))
                .setAssigns(assignBranchs)
                .setAudits(auditBranchs)
                .setLatestUser(taskPo.getLatestUser())
                .setMultiCompany(taskPo.getMultiCompany() != null && taskPo.getMultiCompany().intValue() == Constants.ENABLED)
                .build();
    }

    /**
     * 获取已办详情
     *
     * @param taskId 待办任务ID
     * @return
     */
    public CompletedTaskResponseVO getCompleteTask(Long userId, Long taskId) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = new QueryWrapper<CompleteTaskPO>().lambda().eq(CompleteTaskPO::getId, taskId);
        CompleteTaskPO taskPo = completeTaskMapper.selectOne(queryWrapper);
        if (taskPo == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        if (taskPo.getUserId().longValue() != userId.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_NO_CHECK_PERMISSION_ERROR);
        }
        return voTransfer(taskPo);
    }

    private CompletedTaskResponseVO voTransfer(CompleteTaskPO completeTaskPo) {
        String revoke = bpmnService.getUserTaskAttribute(completeTaskPo.getInstanceId(), Constants.ENABLE_REVOCATION);
        String showlog = bpmnService.getUserTaskAttribute(completeTaskPo.getInstanceId(), Constants.ENABLE_SHOWLOG);
        LambdaQueryWrapper<TaskFormPO> queryWraper = new QueryWrapper<TaskFormPO>().lambda()
                .eq(TaskFormPO::getUserId, completeTaskPo.getUserId())
                .eq(TaskFormPO::getInstanceId, completeTaskPo.getInstanceId());
        TaskFormPO form = taskFormMapper.selectOne(queryWraper);
        return new CompletedTaskResponseVO.Builder()
                .setFormData(form == null ? null : form.getFormData())
                .setUrl(completeTaskPo.getOpenUrl())
                .setProcessName(completeTaskPo.getProcessName())
                .setFormNo(completeTaskPo.getTableNo())
                .setInitiator(completeTaskPo.getStaffName())
                .setSource(completeTaskPo.getTaskSource())
                .setStartTime(completeTaskPo.getStartTime())
                .setTaskId(completeTaskPo.getId().toString())
                .setTaskName(completeTaskPo.getTaskName())
                .setCompleteTime(completeTaskPo.getEndTime())
                .setLatestUser(completeTaskPo.getLatestUser())
                .setRevoke(Boolean.valueOf(revoke))
                .setProcessId(completeTaskPo.getProcessId())
                .setShowlog(Boolean.valueOf(showlog))
                .build();
    }

    /**
     * 待办提交
     *
     * @param taskId   待办任务ID
     * @param formData 表单数据, 可为空
     * @param comment  备注
     * @param assigns  重新指派者列表, 在拦截器中设置到上下文 {@link com.supcon.supfusion.flow.taskcenter.interceptor.OperationInterceptor}
     * @param audit    包含提交分支,备注等信息
     */
    public void submit(Long taskId, String formData, String comment, List<AssigneeRequestVO> assigns, AuditVO audit) {
        Long userId = UserContext.getUserContext().getUserId();
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<PendingTaskPO>().lambda().eq(PendingTaskPO::getId, taskId);
        PendingTaskPO pendingTask = pendingTaskMapper.selectOne(queryWrapper);
        validate(pendingTask, userId.longValue());
        Map<String, Object> transientVariables = buildTransientVariables(formData, audit);
        Map<String, Object> variables = buildVariables(formData, audit);
        // 当前提交人逻辑: 如果当前任务被委托则当前提交人为原始委托人
        Long submitter = pendingTask.getSourceStaff() == null ||  pendingTask.getSourceStaff().longValue() == 0 
                        ? pendingTask.getUserId() 
                        : pendingTask.getSourceStaff();
        try {
            LocalContext.getContext().setProcessId(pendingTask.getProcessId());
            LocalContext.getContext().setSubmitter(submitter);
            ProxyUtils.getProxyObject(TaskCenterService.class).doSubmitWithTransaction(pendingTask, audit, variables, transientVariables);
        } finally {
            LocalContext.getContext().setProcessId(null);
            LocalContext.getContext().setSubmitter(null);
        }
        notificationService.sendNoticeIfConfigure(pendingTask.getProcessId());
        // 记录流程操作日志
        loggerJob.submit(createProcessLogPO(pendingTask, audit, comment));
    }
    
    private void validate(PendingTaskPO taskPo, long userId) {
        if (taskPo == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        if (taskPo.getUserId().longValue() != userId) {
            throw new PermissionException(FlowErrorEnum.TASK_NO_SUBMIT_PERMISSION_ERROR);
        }
        if (taskPo.getTaskStatus() == ProcessStatusEnum.SUSPENDED.getStatus()) {
            throw new StatusAbnormalException(FlowErrorEnum.TASK_STATUS_NOT_ALLOW_COMPLETE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void doSubmitWithTransaction(PendingTaskPO taskPo, AuditVO audit, Map<String, Object> variables, Map<String, Object> transientVariables) {
        String userId = UserContext.getUserContext().getUserId().toString();
        taskEngineService.complete(taskPo.getInstanceId(), taskPo.getProcessId(), userId, variables, transientVariables);
        String formNo = (String)transientVariables.get(Constants.FORM_NO);
        completeTaskMapper.insert(buildCompleteTaskPo(taskPo, formNo));
        String formJson = (String)variables.get(Constants.FORM_DATA);
        // 已办的表单数据
        taskFormMapper.insert(buildTaskForm(taskPo, formJson));
        // 删除委托记录
        LambdaQueryWrapper<EntrustPO> queryWrapper = Wrappers.<EntrustPO>lambdaQuery()
                .eq(EntrustPO::getInstanceId, taskPo.getInstanceId());
        entrustMapper.delete(queryWrapper);
        OodmSettingDTO oodmSetting = bpmnService.getOodmSettings(taskPo.getProcessId(), taskPo.getActivityName());
        if (oodmSetting != null) {
            oodmServiceAdapter.executeService(taskPo, audit, formJson, oodmSetting);
        }
    }

    private TaskFormPO buildTaskForm(PendingTaskPO taskPo, String formJson) {
        TaskFormPO taskForm = new TaskFormPO();
        taskForm.setId(CodeGenerator.generateUUID());
        taskForm.setProcessId(taskPo.getProcessId());
        taskForm.setInstanceId(taskPo.getInstanceId());
        taskForm.setUserId(taskPo.getUserId());
        taskForm.setFormData(formJson);
        taskForm.setCreator(UserContext.getUserContext().getUserName());
        taskForm.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return taskForm;
    }
    
    private CompleteTaskPO buildCompleteTaskPo(PendingTaskPO task, String formNo) {
        OperationTypeEnum operationType = LocalContext.getContext().getOperationType();
        CompleteTaskPO completedTask = new CompleteTaskPO();
        completedTask.setId(task.getId());
        completedTask.setAppId(task.getAppId());
        completedTask.setCid(task.getCid());
        completedTask.setTableNo(formNo);
        completedTask.setInitiatorId(task.getInitiatorId());
        completedTask.setStaffName(task.getStaffName());
        completedTask.setProcessId(task.getProcessId());
        completedTask.setProcessKey(task.getProcessKey());
        completedTask.setProcessName(task.getProcessName());
        completedTask.setProcessVersion(task.getProcessVersion());
        completedTask.setUserId(task.getUserId());
        completedTask.setOpenUrl(task.getOpenUrl());
        completedTask.setTenantId(task.getTenantId());
        completedTask.setTaskSource(task.getTaskSource());
        completedTask.setInstanceId(task.getInstanceId());
        completedTask.setIntegrationId(task.getIntegrationId());
        completedTask.setActivityName(task.getActivityName());
        completedTask.setPersonName(task.getPersonName());
        completedTask.setProxySource(task.getProxySource());
        completedTask.setSourceStaff(task.getSourceStaff());
        completedTask.setStartTime(task.getStartTime());
        completedTask.setTaskName(task.getTaskDescriptionZhCn());
        completedTask.setTaskType(task.getTaskType());
        completedTask.setTenantId(task.getTenantId());
        completedTask.setLatestUser(task.getLatestUser());
        if (operationType == OperationTypeEnum.REJECT) {
            completedTask.setReject(Constants.ENABLED);
        }
        completedTask.setCreator(UserContext.getUserContext().getUserName());
        completedTask.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return completedTask;
    }
    
    private Map<String, Object> buildTransientVariables(String formJson, AuditVO audit) {
        Map<String, Object> transientVariables = MapUtils.jsonToMap(formJson);
        if (audit != null && audit.getValue() != null) {
            transientVariables.put(Constants.AUDIT_VARIABLE, audit.getValue());
        } else {
            // 当只有一个分支时, 待办提交前端不会传audit参数, 此时后端需要给分支一个前进的动力
            transientVariables.put(Constants.AUDIT_VARIABLE, "1"); 
        }
        return transientVariables;
    }

    private Map<String, Object> buildVariables(String formJson, AuditVO audit) {
        Map<String, Object> variables = new HashMap<>(4);
        if (audit != null) {
            variables.put(Constants.AUDIT_DATA, audit);
        }
        variables.put(Constants.FORM_DATA, formJson);
        return variables;
    }
    
    /*private Map<String, Object> buildPersistantVariables(String formJson) {
        Map<String, Object> persistentVariables = new HashMap<>(4);
        
        return persistentVariables;
    } */

    private ProcessLogPO createProcessLogPO(PendingTaskPO taskPo, AuditVO audit, String comment) {
        String userName = UserContext.getUserContext().getUserName();
        String staffName = UserContext.getUserContext().getStaffName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        String auditResult = null;
        String desc = "提交";
        if (audit != null) {
            auditResult = audit.getName();
            desc = audit.getType() == Constants.REJECT ? "驳回" : desc;
        }
        processLog.setLeaveComment(comment);
        processLog.setAuditResult(auditResult == null ? "同意" : auditResult);
        // TODO 国际化
        processLog.setActionDesc(String.format("经由 %s %s", staffName, desc));
        processLog.setProcessId(taskPo.getProcessId());
        processLog.setTaskId(taskPo.getId());
        processLog.setTaskName(taskPo.getTaskDescriptionZhCn());
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(userName);
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.TASK_COMPLETE.name());
        return processLog;
    }

    /**
     * 条件查询委托列表
     *
     * @param queryContract
     * @param pagination    分页信息
     * @return
     */
    public PageResult<EntrustResponseVO> queryEntrust(EntrustQueryContractDTO queryContract, Pagination pagination) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<EntrustPO> listQueryWrapper = TaskQueryWrapper.buildEntrustListQueryWrapper(queryContract, tenantId);
        Integer total = entrustMapper.selectCount(listQueryWrapper);
        Page<EntrustPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<EntrustPO> entrustPagenation = entrustMapper.selectPage(page, listQueryWrapper);
        return new PageResult<>(reconstructEntrusts(entrustPagenation.getRecords())
                , total, entrustPagenation.getSize(), entrustPagenation.getCurrent());
    }

    private List<EntrustResponseVO> reconstructEntrusts(List<EntrustPO> entrustPos) {
        List<EntrustResponseVO> entrustVos = new LinkedList<>();
        Set<String> instanceIds = entrustPos.stream().map(EntrustPO::getInstanceId).collect(Collectors.toSet());
        Map<String, String> showlogMap = bpmnService.batchGetUserTaskAttribute(instanceIds, Constants.ENABLE_SHOWLOG);
        for (EntrustPO entrustPo : entrustPos) {
            TaskFormPO formData = taskFormMapper.selectOne(new QueryWrapper<TaskFormPO>().lambda()
                    .eq(TaskFormPO::getInstanceId, entrustPo.getInstanceId())
                    .eq(TaskFormPO::getUserId, entrustPo.getMandatary()));
            EntrustResponseVO response = new EntrustResponseVO.Builder()
                    .setId(entrustPo.getId().toString())
                    .setEntrustTime(entrustPo.getCreateTime())
                    .setMandataryName(entrustPo.getMandataryName())
                    .setReason(entrustPo.getDescription())
                    .setTaskName(entrustPo.getTaskName())
                    .setProcessName(entrustPo.getProcessName())
                    .setProcessId(entrustPo.getProcessId())
                    .setShowlog(Boolean.valueOf(showlogMap.get(entrustPo.getInstanceId())))
                    .setFormData(formData == null ? null : formData.getFormData())
                    .setFormNo(entrustPo.getTableNo())
                    .build();
            entrustVos.add(response);
        }
        return entrustVos;
    }
    
    /**
     * 获取委托详情
     * @param id
     * @return
     */
    public PendingTaskResponseVO getEntrustDetail(long id) {
        EntrustPO entrustPo = entrustMapper.selectById(id);
        PendingTaskPO pendingTask = pendingTaskMapper.selectById(entrustPo.getTaskId());
        if (pendingTask == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        if (pendingTask.getTaskStatus() == ProcessStatusEnum.SUSPENDED.getStatus()) {
            throw new StatusAbnormalException(FlowErrorEnum.TASK_STATUS_NOT_ALLOW_COMPLETE_ERROR);
        }
        TaskFormPO form = taskFormMapper.selectOne(new QueryWrapper<TaskFormPO>().lambda()
                .eq(TaskFormPO::getInstanceId, entrustPo.getInstanceId())
                .eq(TaskFormPO::getUserId, entrustPo.getMandatary()));
        return new PendingTaskResponseVO.Builder()
                .setFormData(form == null ? null : form.getFormData())
                .setUrl(pendingTask.getOpenUrl())
                .setProcessName(pendingTask.getProcessName())
                .setFormNo(pendingTask.getTableNo())
                .setStartTime(pendingTask.getStartTime())
                .setTaskId(pendingTask.getId().toString())
                .setTaskName(pendingTask.getTaskDescriptionZhCn())
                .setProcessId(pendingTask.getProcessId())
                .setInitiator(pendingTask.getStaffName())
                .setReadonly(Boolean.TRUE)
                .setMultiCompany(pendingTask.getMultiCompany() != null && pendingTask.getMultiCompany().intValue() == Constants.ENABLED)
                .build();
    }

    /**
     * 待办全权委托
     *
     * @param taskId      待办任务ID
     * @param reason      委托原因
     * @param mandatary   受托人
     * @param principal   委托人
     */
    public void entrust(String taskId, String reason, Long mandatary, Long principal) {
        PendingTaskPO taskPO = pendingTaskMapper.selectById(taskId);
        if (taskPO == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        validate(taskPO, mandatary, principal);
        ProxyUtils.getProxyObject(TaskCenterService.class).doEntrustWithTransaction(reason, taskPO, mandatary, principal);
        // 记录操作日志
        String principalName = UserContext.getUserContext().getStaffName();
        String mandataryName = userServiceAdapter.getPersonName(mandatary);
        notificationService.sendNoticeIfConfigure(Collections.singletonList(taskPO));
        loggerJob.submit(createProcessLogPO(taskPO, reason, mandataryName, principalName));
    }
    
    /**
     * 代理待办全权委托
     *
     * @param taskId      待办任务ID
     * @param reason      委托原因
     * @param mandatary   受托人
     */
    public void proxyEntrust(Long taskId, String reason, Long mandatary) {
        PendingTaskPO taskPO = pendingTaskMapper.selectById(taskId);
        QueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.COL_USER_ID, mandatary);
        queryWrapper.eq(Constants.COL_INSTANCE_ID, taskPO.getInstanceId());
        Integer count = pendingTaskMapper.selectCount(queryWrapper);
        // 判断被委托者是否已经拥有待办
        if (count != null && count.intValue() > 0) {
            throw new UniqueConstraintsException(FlowErrorEnum.TASK_ENTRUST_OWNER_NOT_ALLOW_ERROR);
        }
        String owner = taskPO.getPersonName();
        ProxyUtils.getProxyObject(TaskCenterService.class).doEntrustWithTransaction(reason, taskPO, mandatary, taskPO.getUserId());
        notificationService.sendNoticeIfConfigure(Collections.singletonList(taskPO));
        // 记录操作日志
        loggerJob.submit(createProcessLogPO(taskPO, reason, taskPO.getPersonName(), "管理员 代理 " + owner));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void doEntrustWithTransaction(String reason, PendingTaskPO taskPO, Long mandatary, Long principal) {
        EntrustPO entrustPO = createEntrustPO(reason, taskPO, mandatary, principal);
        entrustMapper.insert(entrustPO);
        pendingTaskMapper.deleteTask(taskPO);
        taskPO.setUserId(mandatary);
        taskPO.setPersonName(entrustPO.getMandataryName());
        if (taskPO.getSourceStaff() == null || taskPO.getSourceStaff() == 0L) {
            taskPO.setSourceStaff(principal); // 原始委托者
        }
        taskPO.setProxySource(principal); // 最近一次委托者
        pendingTaskMapper.insert(taskPO);
        LambdaQueryWrapper<TaskFormPO> queryWrapper = Wrappers.<TaskFormPO>lambdaQuery()
                .eq(TaskFormPO::getUserId, principal)
                .eq(TaskFormPO::getInstanceId, taskPO.getInstanceId());
        TaskFormPO newForm = new TaskFormPO();
        newForm.setUserId(mandatary);
        taskFormMapper.update(newForm, queryWrapper);
    }

    private ProcessLogPO createProcessLogPO(PendingTaskPO taskPO, String reason, String mandataryName, String principalName) {
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        processLog.setProcessId(taskPO.getProcessId());
        processLog.setProcessName(taskPO.getProcessName());
        processLog.setTaskId(taskPO.getId());
        processLog.setTaskName(taskPO.getTaskDescriptionZhCn());
        processLog.setActionDesc(String.format("经由 %s 委托给 %s", principalName, mandataryName));
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setLeaveComment(reason);
        processLog.setCreator(UserContext.getUserContext().getUserName());
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.TASK_DELEGATE.name());
        return processLog;
    }

    private void validate(PendingTaskPO taskPo, Long mandatary, Long principal) {
        if (taskPo.getUserId().longValue() != principal.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_PERMISSION_DENY_ERROR);
        }
        // 暂停的待办无法委托
        if (taskPo.getTaskStatus() == ProcessStatusEnum.SUSPENDED.getStatus()) {
            throw new StatusAbnormalException(FlowErrorEnum.TASK_STATUS_NOT_ALLOW_ENTRUST_ERROR);
        }
        EntrustPO entrustPo = entrustMapper.selectOne(TaskQueryWrapper.buildEntrustQueryWrapper(taskPo.getId(), null, mandatary));
        // 记录已存在, 不能重复委托
        if (isDuplicateEntrust(entrustPo)) {
            throw new PermissionException(FlowErrorEnum.TASK_DUPLICATE_ENTRUST_ERROR);
        }
        QueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.COL_USER_ID, mandatary);
        queryWrapper.eq(Constants.COL_INSTANCE_ID, taskPo.getInstanceId());
        Integer count = pendingTaskMapper.selectCount(queryWrapper);
        // 判断被委托者是否已经拥有待办
        if (count != null && count.intValue() > 0) {
            throw new PermissionException(FlowErrorEnum.TASK_ENTRUST_OWNER_NOT_ALLOW_ERROR);
        }
        UserOrgDetailDTO mandataryUser = userServiceAdapter.getUserById(mandatary);
        if (mandataryUser == null) {
            throw new NotExistException(FlowErrorEnum.MANDATARY_USER_NOT_EXIST_ERROR);
        }
    }

    // 判断是否重复委托
    private boolean isDuplicateEntrust(EntrustPO entrustPo) {
        return entrustPo != null;
    }

    private EntrustPO createEntrustPO(String reason, PendingTaskPO taskPo, Long mandatary, Long principal) {
        String tenantId = RpcContext.getContext().getTenantId();
        String mandataryName = userServiceAdapter.getPersonName(mandatary);
        EntrustPO entrustPo = new EntrustPO();
        entrustPo.setCid(taskPo.getCid());
        entrustPo.setId(CodeGenerator.generateUUID());
        entrustPo.setAppId(taskPo.getAppId());
        entrustPo.setTableNo(taskPo.getTableNo());
        entrustPo.setInstanceId(taskPo.getInstanceId());
        entrustPo.setDescription(reason);
        entrustPo.setMandatary(mandatary);
        entrustPo.setMandataryName(mandataryName);
        entrustPo.setPrincipal(principal);
        entrustPo.setProcessId(taskPo.getProcessId());
        entrustPo.setProcessName(taskPo.getProcessName());
        entrustPo.setTaskId(taskPo.getId());
        entrustPo.setTaskName(taskPo.getTaskDescriptionZhCn());
        entrustPo.setTenantId(tenantId);
        entrustPo.setCreator(UserContext.getUserContext().getUserName());
        entrustPo.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return entrustPo;
    }

    /**
     * 取消委托
     *
     * @param id 记录ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelEntrust(long id) {
        EntrustPO entrustPo = entrustMapper.selectById(id);
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = TaskQueryWrapper.buildIdQueryWrapper(entrustPo.getTaskId());
        PendingTaskPO taskPo = pendingTaskMapper.selectOne(queryWrapper);
        EntrustPO entrustNode = entrustPo; // 链式委托的头节点
        while (entrustNode != null) {
            // 更新委托状态为已取消
            entrustMapper.deleteById(entrustNode.getId());
            // A->B->C 假设A取消委托, 那么B->C也应该取消
            entrustNode = entrustMapper.selectOne(TaskQueryWrapper.buildEntrustQueryWrapper(entrustNode.getTaskId(), entrustNode.getMandatary(), null));
        }
        // 更新表单数据拥有者
        LambdaQueryWrapper<TaskFormPO> updateWrapper = Wrappers.<TaskFormPO>lambdaQuery()
                .eq(TaskFormPO::getUserId, taskPo.getUserId())
                .eq(TaskFormPO::getInstanceId, taskPo.getInstanceId());
        TaskFormPO newForm = new TaskFormPO();
        newForm.setUserId(UserContext.getUserContext().getUserId());
        taskFormMapper.update(newForm, updateWrapper);
        pendingTaskMapper.deleteTask(taskPo);
        // 更新待办状态
        taskPo.setUserId(UserContext.getUserContext().getUserId());
        taskPo.setPersonName(UserContext.getUserContext().getStaffName());
        taskPo.setProxySource(UserContext.getUserContext().getUserId()); // 最近一次委托者
        pendingTaskMapper.insert(taskPo);
        loggerJob.submit(createCancelEntrustLogPO(entrustPo));
    }

    private ProcessLogPO createCancelEntrustLogPO(EntrustPO entrustPo) {
        String staffName = UserContext.getUserContext().getStaffName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        processLog.setProcessId(entrustPo.getProcessId());
        processLog.setTaskId(entrustPo.getTaskId());
        processLog.setTaskName(entrustPo.getTaskName());
        processLog.setActionDesc(String.format("经由 %s 取消委托", staffName));
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(UserContext.getUserContext().getUserName());
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.TASK_CANCEL_DELEGATE.name());
        return processLog;
    }

    /**
     * 撤回
     *
     * @param taskId 已完成的待办任务ID
     */
    public void revoke(Long taskId) throws DocumentException {
        CompleteTaskPO completedTask = completeTaskMapper.getCompleteTask(taskId);
        if (completedTask == null) {
            throw new NotExistException(FlowErrorEnum.COMPLETED_TASK_NOT_EXIST_ERROR);
        }
        Long userId = UserContext.getUserContext().getUserId();
        if (completedTask.getUserId().longValue() != userId.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_REVOKE_DENY_ERROR);
        }
        Set<String> currentUserTaskKeys = validate(completedTask);
        ProxyUtils.getProxyObject(TaskCenterService.class).doRevokeWithTransaction(completedTask, currentUserTaskKeys);
        // 记录日志
        loggerJob.submit(createRevokeLogPO(completedTask));
    }

    /**
     * 验证条件:
     * 1. 只有进行中的流程才能撤回
     * 2. 会签环节不能撤回
     * 3. 如果当前环节的目标人工节点不存在时不能撤回
     * 4. 如果对方任务已经提交不能撤回
     * 5. 撤回路径如果经过并行网关不能撤回
     */
    private Set<String> validate(CompleteTaskPO completedTask) throws DocumentException {
        ProcessPO process = processService.getProcess(completedTask.getProcessId());
        int processStatus = process.getProcessStatus().intValue();
        if (processStatus == ProcessStatusEnum.COMPLETED.getStatus()
                || processStatus == ProcessStatusEnum.CANCELLED.getStatus()) {
            throw new TaskRuntimeException(FlowErrorEnum.PROCESS_STATUS_NOT_ALLOW_REVOKE_ERROR);
        }
        if (processStatus == ProcessStatusEnum.SUSPENDED.getStatus()) {
            throw new TaskRuntimeException(FlowErrorEnum.PROCESS_SUSPENDED_ERROR);
        }
        // 会签环节不能撤回
        boolean isLoop = bpmnService.isLoopTask(completedTask.getProcessId(), completedTask.getActivityName());
        if (isLoop) {
            throw new TaskRuntimeException(FlowErrorEnum.LOOP_TASK_NOT_REVOKE);
        }
        List<ElementDTO> targets = bpmnService.queryNextUserTaskKey(completedTask.getProcessId(), completedTask.getActivityName());
        if (targets.isEmpty()) {
            throw new TaskRuntimeException(FlowErrorEnum.LAST_TASK_CANNOT_REVOKE);
        }
        // 判断下一个环节是不是并行网关
        List<PendingTaskPO> activeTasks = pendingTaskMapper.selectList(new QueryWrapper<PendingTaskPO>().lambda()
                .eq(PendingTaskPO::getProcessId, completedTask.getProcessId()));
        for (PendingTaskPO activeTask : activeTasks) {
            boolean crossParallelGateway = bpmnService.isCrossParallelGateway(completedTask.getProcessId(), completedTask.getInstanceId(), activeTask.getInstanceId());
            if (crossParallelGateway) {
                throw new TaskRuntimeException(FlowErrorEnum.TASK_REVOKE_FAIL);
            }
        }
        // 判断对方是否已经提交了待办
        Set<String> targetIds = targets.stream().map(ElementDTO::getTargetId).collect(Collectors.toSet());
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<PendingTaskPO>()
                .select("distinct instance_id, activity_name").lambda()
                .eq(PendingTaskPO::getProcessId, completedTask.getProcessId())
                .in(PendingTaskPO::getActivityName, targetIds);
        List<PendingTaskPO> pendingTasks = pendingTaskMapper.selectList(queryWrapper);
        if (pendingTasks.isEmpty()) {
            throw new TaskRuntimeException(FlowErrorEnum.STATUS_CHANGED_REVOKE_FAIL);
        }
        return pendingTasks.stream().map(PendingTaskPO::getActivityName).collect(Collectors.toSet());
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void doRevokeWithTransaction(CompleteTaskPO completedTask, Set<String> currentUserTaskKeys) {
        Map<String, Object> variables = new HashMap<>();
        if (completedTask.getFormData() != null) {
            variables = MapUtils.jsonToMap(completedTask.getFormData());
            variables.put(Constants.FORM_DATA, completedTask.getFormData());
        }
        taskEngineService.revoke(completedTask.getInstanceId(), completedTask.getProcessId(), currentUserTaskKeys, variables);
        completeTaskMapper.deleteById(completedTask.getId());
        taskFormMapper.deleteFormData(completedTask.getInstanceId());
        // 重新将委托者加回去, 在提交给领导的时候需要判断是谁的领导
        PendingTaskPO reborn = new PendingTaskPO();
        reborn.setSourceStaff(completedTask.getSourceStaff());
        reborn.setProxySource(completedTask.getProxySource());
        pendingTaskMapper.update(reborn, new QueryWrapper<PendingTaskPO>().lambda()
                .eq(PendingTaskPO::getActivityName, completedTask.getActivityName())
                .eq(PendingTaskPO::getProcessId, completedTask.getProcessId()));
    }

    private ProcessLogPO createRevokeLogPO(CompleteTaskPO completedTask) {
        String staffName = UserContext.getUserContext().getStaffName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        processLog.setProcessId(completedTask.getProcessId());
        processLog.setTaskId(completedTask.getId());
        processLog.setTaskName(completedTask.getTaskName());
        processLog.setActionDesc(String.format("经由 %s 撤回", staffName));
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(UserContext.getUserContext().getUserName());
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.TASK_ROLLBACK.name());
        return processLog;
    }


    /**
     * 加签
     *
     * @param invitees     受邀人列表 -- 编号
     * @param taskId      待办任务ID
     */
    public void joinMultiTask(List<String> invitees, Long taskId) {
        PendingTaskPO taskPo = pendingTaskMapper.selectById(taskId);
        if (taskPo == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        List<PendingTaskPO> tasks = listPendingTaskByProcess(taskPo.getProcessId(), false);
        List<String> taskUsers = tasks.stream().map(pt -> pt.getUserId().toString()).collect(Collectors.toList());
        for (String invitee : invitees) {
            if (taskUsers.contains(invitee)) {
                throw new TaskRuntimeException(FlowErrorEnum.TASK_JOIN_DUPLICATE);
            }
        }
        List<String> taskInstanceIds = taskEngineService.joinMultiTask(taskPo.getInstanceId(), invitees, new HashMap<>());
        List<PendingTaskPO> newTasks = pendingTaskMapper.selectList(new QueryWrapper<PendingTaskPO>().lambda().in(PendingTaskPO::getInstanceId, taskInstanceIds));
        notificationService.sendNoticeIfConfigure(newTasks);
        // 记录操作日志
        loggerJob.submit(createJoinLogPO(taskPo, invitees));
    }

    private ProcessLogPO createJoinLogPO(PendingTaskPO taskPo, List<String> invitees) {
        List<String> inviteeName = new ArrayList<>();
        for (String invitee : invitees) {
            String personName = userServiceAdapter.getPersonName(Long.parseLong(invitee));
            inviteeName.add(personName);
        }
        String staffName = UserContext.getUserContext().getStaffName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        processLog.setProcessId(taskPo.getProcessId());
        processLog.setTaskId(taskPo.getId());
        processLog.setTaskName(taskPo.getTaskDescriptionZhCn());
        // TODO 国际化
        processLog.setActionDesc(String.format("%s 邀请 %s 加入会签", staffName, String.join(",", inviteeName)));
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(staffName);
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.TASK_ADD.name());
        return processLog;
    }

    /**
     * 迁移待办
     *
     * @param taskId        待办ID
     * @param targetTaskKey 目标待办key
     * @return
     */
    public void migrateTask(Long taskId, String targetTaskKey) {
        PendingTaskPO taskPo = pendingTaskMapper.selectById(taskId);
        if (taskPo == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        taskEngineService.migrate(taskPo.getInstanceId(), taskPo.getProcessId(), targetTaskKey);
    }

    /**
     * 根据流程ID删除待办
     * @param processIds
     */
    public void deletePendingTask(List<String> processIds) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = new QueryWrapper<PendingTaskPO>().lambda().in(PendingTaskPO::getProcessId, processIds);
        pendingTaskMapper.delete(queryWrapper);
    }
    /**
     * 根据流程ID删除待办
     * @param processIds
     */
    public void deleteCompleteTask(List<String> processIds) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = new QueryWrapper<CompleteTaskPO>().lambda().in(CompleteTaskPO::getProcessId, processIds);
        completeTaskMapper.delete(queryWrapper);
    }
    /**
     * 删除委托
     * @param processIds
     */
    public void deleteEntrust(List<String> processIds) {
        LambdaQueryWrapper<EntrustPO> queryWrapper = new QueryWrapper<EntrustPO>().lambda().in(EntrustPO::getProcessId, processIds);
        entrustMapper.delete(queryWrapper);
    }
    
    /**
     * 相同待办合并
     * @param processId
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void combineTask(String processId) {
        List<PendingTaskPO> tasks = pendingTaskMapper.queryTaskByProcess(processId);
        Map<String, List<PendingTaskPO>> groupTasks = tasks.stream().collect(Collectors.groupingBy(t -> 
                String.format("%s,%s,%s,%s", t.getProcessId(),t.getActivityName(), t.getUserId(), t.getTableNo())));
        for (Map.Entry<String, List<PendingTaskPO>> entry : groupTasks.entrySet()) {
            int size = entry.getValue().size();
            if (size > 1) {
                String latestTaskInstanceId = taskEngineService.migrate(processId, entry.getValue().get(0).getActivityName(), entry.getValue().get(0).getActivityName());
                // 删除多余的待办, 更新最新的待办数据
                for (int i = 1; i < size; i++) {
                    pendingTaskMapper.deleteById(entry.getValue().get(i).getId());
                }
                PendingTaskPO newTask = new PendingTaskPO();
                newTask.setId(entry.getValue().get(0).getId());
                newTask.setInstanceId(latestTaskInstanceId);
                pendingTaskMapper.updateById(newTask);
                
                TaskFormPO newForm = new TaskFormPO();
                newForm.setInstanceId(latestTaskInstanceId);
                taskFormMapper.update(newForm, new UpdateWrapper<TaskFormPO>().lambda().eq(TaskFormPO::getInstanceId, entry.getValue().get(0).getInstanceId()));
            }
        }
    }
}
