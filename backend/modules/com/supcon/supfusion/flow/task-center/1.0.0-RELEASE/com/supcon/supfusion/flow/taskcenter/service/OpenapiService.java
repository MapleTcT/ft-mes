/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.supcon.supfusion.flow.common.po.*;
import com.supcon.supfusion.flow.common.vo.openapi.*;
import com.supcon.supfusion.flow.dao.*;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.dto.AuditDTO;
import com.supcon.supfusion.flow.common.dto.DiagramDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.exception.PermissionException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.taskcenter.rpc.NotificationService;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;

/**
 * @author: zhuangmh
 * @date: 2020年11月10日 下午3:14:58
 */
@Service
public class OpenapiService {
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    @Autowired
    private CompleteTaskMapper completeTaskMapper;
    @Autowired
    private TaskCenterService taskCenterService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private TaskFormMapper taskFormMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ProcessLogMapper processLogMapper;
    /**
     * 查询我的发起
     * @param userId
     * @param pagination
     * @return
     */
    public PageResult<ProcessVO> queryProcess(String appId, Long userId, int status, Pagination pagination) {
        LambdaQueryWrapper<ProcessPO> processQuery =  Wrappers.<ProcessPO>lambdaQuery().eq(ProcessPO::getUserId, userId);
        if (status > 0) {
            ProcessStatusEnum statusEnum = ProcessStatusEnum.getStatusEnum(status);
            if (statusEnum == null) {
                throw new NotExistException(FlowErrorEnum.PROCESS_STATUS_NOT_EXIST_FAIL);
            }
            processQuery.eq(ProcessPO::getProcessStatus, status);
        }
        if (appId != null) {
            processQuery.eq(ProcessPO::getAppId, appId);
        }
        Integer total = processMapper.selectCount(processQuery);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        processQuery.orderByDesc(ProcessPO::getCreateTime);
        Page<ProcessPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<ProcessPO> processPages = processMapper.selectPage(page, processQuery);
        if (processPages.getRecords() == null || processPages.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        return new PageResult<>(processVOTransfer(processPages.getRecords()), total, pagination.getPageSize(), pagination.getCurrent());
       
    }
    
    private List<ProcessVO> processVOTransfer(List<ProcessPO> records) {
        List<ProcessVO> processList = new LinkedList<>();
        for (ProcessPO record : records) {
            ProcessVO process = reconstructPO(record, true);
            processList.add(process);
        }
        return processList;
    }
    
    private ProcessVO reconstructPO(ProcessPO record, boolean containUser) {
        List<PendingTaskPO> tasks = taskCenterService.listPendingTaskByProcess(record.getId().toString(), false);
        ProcessVO process = new ProcessVO();
        process.setAppId(record.getAppId());
        process.setProcessId(record.getId().toString());
        process.setProcessName(record.getProcessName());
        process.setStartTime(record.getCreateTime());
        process.setEndTime(record.getCompleteTime());
        process.setStatus(record.getProcessStatus());
        List<SimpleTaskVO> sts = new ArrayList<>();
        for (PendingTaskPO t : tasks) {
            SimpleTaskVO st = new SimpleTaskVO();
            st.setTaskId(t.getId().toString());
            st.setTaskName(t.getTaskDescriptionZhCn());
            st.setPersonName(t.getPersonName());
            if (containUser) {
                String userName = userServiceAdapter.getUserName(t.getUserId());
                st.setUsername(userName);
            }
            sts.add(st);
        }
        process.setTasks(sts);
        return process;
    }
    /**
     * 
     * @param processId
     * @return
     */
    public List<UrgeInfoVO> listUrgeInfo(String processId) {
        ProcessPO process = processMapper.selectById(processId);
        if (process == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        List<PendingTaskPO> tasks = taskCenterService.listPendingTaskByProcess(processId, true);
        Map<String, List<PendingTaskPO>> groupTasks = tasks.stream().collect(Collectors.groupingBy(PendingTaskPO::getInstanceId));
        List<UrgeInfoVO> urgeInfos = new ArrayList<>();
        List<ProtocolDTO> protocols = notificationService.retrieveProtocols(Constants.TASK_URGE_TOPIC);
        List<ProtocolVO> protocolsVO = new LinkedList<>();
        for (ProtocolDTO protocol : protocols) {
            protocolsVO.add(new ProtocolVO(protocol.getProtocol(), protocol.getName()));
        }
        for (Map.Entry<String, List<PendingTaskPO>> entry : groupTasks.entrySet()) {
            UrgeInfoVO urgeInfo = new UrgeInfoVO();
            urgeInfo.setProtocols(protocolsVO);
            urgeInfo.setTaskId(entry.getValue().get(0).getId().toString());
            urgeInfo.setTaskName(entry.getValue().get(0).getTaskDescriptionZhCn());
            urgeInfo.setFormNo(entry.getValue().get(0).getTableNo());
            urgeInfo.setUrl(entry.getValue().get(0).getOpenUrl());
            urgeInfo.setFormData(entry.getValue().get(0).getFormData());
            urgeInfo.setStartTime(entry.getValue().get(0).getStartTime());
            List<SimpleUserVO> users = new ArrayList<>();
            for (PendingTaskPO pendingTask : entry.getValue()) {
                String username = userServiceAdapter.getUserName(pendingTask.getUserId());
                users.add(new SimpleUserVO(username, pendingTask.getPersonName()));
            }
            urgeInfo.setUsers(users);
            urgeInfos.add(urgeInfo);
        }
        return urgeInfos;
    }

    /**
     * 查询流程操作记录
     *
     * @param processId
     *            流程实例ID
     * @return
     */
    public List<ProcessLogVO> queryProcessLogs(String processId) {
        ProcessPO process = processMapper.selectById(processId);
        if (process == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        LambdaQueryWrapper<ProcessLogPO> queryWrapper = Wrappers.<ProcessLogPO>lambdaQuery().eq(ProcessLogPO::getProcessId, processId).orderByDesc(ProcessLogPO::getCreateTime);
        List<ProcessLogPO> logEntities = processLogMapper.selectList(queryWrapper);
        List<ProcessLogVO> logVos = new LinkedList<>();
        if (logEntities.isEmpty()) {
            return logVos;
        }
        Set<String> usernames = logEntities.stream().map(ProcessLogPO::getCreator).collect(Collectors.toSet());
        Map<String, UserDetailDTO> userMap = userServiceAdapter.batchQueryUserByName(usernames);
        for (ProcessLogPO processLog : logEntities) {
            ProcessLogVO logVO = new ProcessLogVO();
            logVO.setAuditResult(processLog.getAuditResult());
            logVO.setComment(processLog.getLeaveComment());
            logVO.setCreateTime(processLog.getCreateTime());
            // TODO 国际化
            logVO.setOperateDesc(processLog.getActionDesc());
            logVO.setTaskName(processLog.getTaskName());
            UserDetailDTO userDetail = userMap.get(processLog.getCreator());
            logVO.setOperator(userDetail == null ? "" : userDetail.getPersonName());
            logVO.setType(processLog.getActionType());
            logVos.add(logVO);
        }
        return logVos;
    }
    
    /**
     * 获取流程详情
     * @param processId
     * @return
     */
    public ProcessVO getProcess(String processId) {
        ProcessPO process = processMapper.selectById(processId);
        if (process == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        Long userId = UserContext.getUserContext().getUserId();
        if (process.getUserId().longValue() != userId.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_NO_CHECK_PERMISSION_ERROR);
        }
        return reconstructPO(process, true);
    }
    
    /**
     * 获取流程启动信息
     * @param appId
     * @param processKey
     * @return
     * @throws DocumentException
     */
    public ProcessStartInfoVO getProcessStartInfo(String appId, String processKey) throws DocumentException {
        com.supcon.supfusion.flow.common.vo.webapi.ProcessStartInfoVO processStartInfo = processService.getProcessStartInfo(appId, processKey);
        ProcessStartInfoVO result = new ProcessStartInfoVO();
        if (processStartInfo.getAssigns() != null) {
            List<AssigneeVO> assigns = new ArrayList<>();
            for (com.supcon.supfusion.flow.common.vo.webapi.AssigneeVO assign : processStartInfo.getAssigns()) {
                assigns.add(new AssigneeVO(assign.getId(), assign.getName(), assign.getTaskDefKey()));
            }
            result.setAssigns(assigns);
        }
        if (processStartInfo.getAudits() != null) {
            List<AuditVO> audits = new ArrayList<>();
            for (com.supcon.supfusion.flow.common.vo.webapi.AuditVO audit : processStartInfo.getAudits()) {
                AuditVO auditVO = new AuditVO();
                auditVO.setSeqKey(audit.getId());
                auditVO.setOrder(audit.getOrder());
                auditVO.setName(audit.getName());
                auditVO.setType(audit.getType());
                auditVO.setValue(audit.getValue());
                audits.add(auditVO);
            }
            result.setAudits(audits);
        }
        result.setCompanyId(processStartInfo.getCompanyId());
        result.setEnableComment(processStartInfo.getEnableComment());
        result.setEnableSave(processStartInfo.getEnableSave());
        result.setMultiCompany(processStartInfo.getMultiCompany());
        result.setProcessName(processStartInfo.getProcessName());
        result.setReadOnly(processStartInfo.getReadOnly());
        result.setFirstTaskName(processStartInfo.getStartTaskName());
        result.setUrl(processStartInfo.getUrl());
        return result;
    }
    
    /**
     * 启动流程
     * @param diagramDto
     * @param formData
     * @param comment
     * @param assigns
     * @param auditRequest
     * @return
     * @throws DocumentException
     */
    public String startProcess(DiagramDTO diagramDto, String formData, String comment, List<AssigneeRequestVO> assigns
            , AuditRequestVO auditRequest) throws DocumentException {
        com.supcon.supfusion.flow.common.vo.webapi.AuditVO auditInner = null;
        if (auditRequest != null) {
            auditInner = new com.supcon.supfusion.flow.common.vo.webapi.AuditVO();
            auditInner.setId(auditRequest.getSeqKey());
            auditInner.setValue(auditRequest.getValue());
        }
        List<com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO> assignsInners = new ArrayList<>();
        if (assigns != null) {
            for (AssigneeRequestVO assign : assigns) {
                com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO assignsInner = new com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO();
                Set<String> usernames = assign.getUsers();
                Set<String> userIds = new HashSet<>();
                for (String username : usernames) {
                    UserOrgDetailDTO user = userServiceAdapter.getUserByName(username);
                    if (user != null) {
                        userIds.add(user.getId().toString());
                    }
                }
                assignsInner.setUsers(userIds);
                assignsInner.setTaskDefKey(assign.getTaskDefKey());
                assignsInners.add(assignsInner);
            }
        }
        return processService.startProcess(diagramDto , formData, comment, assignsInners, auditInner);
    }
    
    /**
     * 查询我的待办列表
     *
     * @param
     * @return
     */
    public PageResult<PendingTaskListResponseVO> queryPendingTask(String appId, Long userId, Pagination pagination) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery();
        queryWrapper.eq(PendingTaskPO::getUserId, userId);
        if (appId != null) {
            queryWrapper.eq(PendingTaskPO::getAppId, appId);
        }
        Integer total = pendingTaskMapper.selectCount(queryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        queryWrapper.orderByDesc(PendingTaskPO::getCreateTime);
        Page<PendingTaskPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<PendingTaskPO> tasks = pendingTaskMapper.selectPage(page, queryWrapper);
        if (tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        List<PendingTaskListResponseVO> taskVos = voListTransfer(tasks.getRecords());
        return new PageResult<>(taskVos, total, pagination.getPageSize(), pagination.getCurrent());
    }
    
    /**
     * 待办详情
     * @param userId
     * @param taskId
     * @return
     * @throws DocumentException
     */
    public PendingTaskResponseVO getPendingTask(Long userId, Long taskId) throws DocumentException {
        com.supcon.supfusion.flow.common.vo.webapi.PendingTaskResponseVO pendingTask = taskCenterService.getPendingTask(userId, taskId);
        List<AuditVO> audits = new ArrayList<>();
        List<AssigneeVO> assigns = new ArrayList<>();
        if (pendingTask.getAudits() != null) {
            for ( com.supcon.supfusion.flow.common.vo.webapi.AuditVO audit : pendingTask.getAudits()) {
                AuditVO a = new AuditVO();
                a.setName(audit.getName());
                a.setOrder(audit.getOrder());
                a.setSeqKey(audit.getId());
                a.setType(audit.getType());
                a.setValue(audit.getValue());
                audits.add(a);
            }
        }
        if (pendingTask.getAssigns() != null) {
            for (com.supcon.supfusion.flow.common.vo.webapi.AssigneeVO assign : pendingTask.getAssigns()) {
                AssigneeVO assignee = new AssigneeVO(assign.getId(),assign.getName(), assign.getTaskDefKey());
                assigns.add(assignee);
            }
        }
        return new PendingTaskResponseVO.Builder()
                .setAddInstance(pendingTask.getAddInstance())
                .setAppId(pendingTask.getAppId())
                .setAssigns(assigns)
                .setAudits(audits)
                .setCompanyId(pendingTask.getCompanyId())
                .setEnableComment(pendingTask.getEnableComment())
                .setFormData(pendingTask.getFormData())
                .setFormTempData(pendingTask.getFormTempData())
                .setInitiator(pendingTask.getInitiator())
                .setMultiCompany(pendingTask.getMultiCompany())
                .setProcessId(pendingTask.getProcessId())
                .setProcessName(pendingTask.getProcessName())
                .setReadonly(pendingTask.getReadonly())
                .setShowlog(pendingTask.getShowlog())
                .setStartTime(pendingTask.getStartTime())
                .setStatus(pendingTask.getStatus())
                .setTaskId(pendingTask.getTaskId())
                .setTaskName(pendingTask.getTaskName())
                .setUrl(pendingTask.getUrl())
                .build();
    }
    
    /**
     * 待办任务提交
     * @param submitRequest
     * @throws DocumentException 
     */
    public void submitTask(TaskSubmitRequestVO submitRequest) throws DocumentException {
        com.supcon.supfusion.flow.common.vo.webapi.AuditVO auditInner = null;
        PendingTaskPO task = pendingTaskMapper.selectById(submitRequest.getTaskId());
        if (task == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        // 验证提交分支是否存在
        List<AuditDTO> audits = bpmnService.listAudit(task.getInstanceId(), task.getProcessId());
        if (audits.size() > 1 && submitRequest.getAudit() == null) {
            if (submitRequest.getAudit() == null) {
                throw new NotExistException(FlowErrorEnum.AUDIT_BRANCH_REQUIRED);
            } else {
                for (AuditDTO audit : audits) {
                    if (audit.getId().equals(submitRequest.getAudit().getSeqKey())) {
                        auditInner = new com.supcon.supfusion.flow.common.vo.webapi.AuditVO();
                        auditInner.setId(submitRequest.getAudit().getSeqKey());
                        auditInner.setValue(submitRequest.getAudit().getValue());
                        auditInner.setName(audit.getName());
                        auditInner.setOrder(audit.getOrder());
                        auditInner.setType(audit.getType());
                        break;
                    }
                }
                if (auditInner == null) {
                    throw new NotExistException(FlowErrorEnum.SEQUENCE_NOT_EXIST);
                }
            }
        }
        List<com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO> assignsInners = new ArrayList<>();
        if (submitRequest.getAssigns() != null) {
            for (AssigneeRequestVO assign : submitRequest.getAssigns()) {
                com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO assignsInner = new com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO();
                Set<String> usernames = assign.getUsers();
                Set<String> userIds = new HashSet<>();
                for (String username : usernames) {
                    UserOrgDetailDTO user = userServiceAdapter.getUserByName(username);
                    if (user != null) {
                        userIds.add(user.getId().toString());
                    }
                }
                assignsInner.setUsers(userIds);
                assignsInner.setTaskDefKey(assign.getTaskDefKey());
                assignsInners.add(assignsInner);
            }
        }
        taskCenterService.submit(Long.parseLong(submitRequest.getTaskId()), submitRequest.getFormData(), submitRequest.getComment()
                , assignsInners, auditInner);
    }
    
    /**
     * 查询我的已办列表
     *
     * @param
     * @return
     */
    public PageResult<CompleteTaskListResponseVO> queryCompletive(String appId, Long userId, Pagination pagination) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = Wrappers.<CompleteTaskPO>lambdaQuery();
        queryWrapper.eq(CompleteTaskPO::getUserId, userId);
        if (appId != null) {
            queryWrapper.eq(CompleteTaskPO::getAppId, appId);
        }
        Integer total = completeTaskMapper.selectCount(queryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        queryWrapper.orderByDesc(CompleteTaskPO::getCreateTime);
        Page<CompleteTaskPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<CompleteTaskPO> tasks = completeTaskMapper.selectPage(page, queryWrapper);
        List<CompleteTaskListResponseVO> taskVos = completiveTaskTransfer(tasks.getRecords());
        return new PageResult<>(taskVos, total, pagination.getPageSize(), pagination.getCurrent());
    }

    private List<CompleteTaskListResponseVO> completiveTaskTransfer(List<CompleteTaskPO> taskPos) {
        List<CompleteTaskListResponseVO> voList = new LinkedList<>();
        for (CompleteTaskPO task : taskPos) {
            CompleteTaskListResponseVO vo = new CompleteTaskListResponseVO.Builder()
                    .setEndTime(task.getEndTime())
                    .setFormNo(task.getTableNo())
                    .setInitiator(task.getStaffName())
                    .setProcessId(task.getProcessId())
                    .setProcessName(task.getProcessName())
                    .setReject(task.getReject() == Constants.ENABLED)
                    .setStartTime(task.getStartTime())
                    .setTaskId(task.getId().toString())
                    .setTaskName(task.getTaskName())
                    .setUrl(task.getOpenUrl())
                    .build();
            voList.add(vo);
        }
        return voList;
    }
    
    public CompletedTaskResponseVO getCompleteTask(Long userId, Long taskId) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = new QueryWrapper<CompleteTaskPO>().lambda().eq(CompleteTaskPO::getId, taskId);
        CompleteTaskPO taskPo = completeTaskMapper.selectOne(queryWrapper);
        if (taskPo == null) {
            throw new NotExistException(FlowErrorEnum.COMPLETED_TASK_NOT_EXIST_ERROR);
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
        UserOrgDetailDTO userInfo = userServiceAdapter.getUserById(Long.parseLong(completeTaskPo.getInitiatorId()));
        return new CompletedTaskResponseVO.Builder()
                .setFormData(form == null ? null : form.getFormData())
                .setUrl(completeTaskPo.getOpenUrl())
                .setProcessName(completeTaskPo.getProcessName())
                .setFormNo(completeTaskPo.getTableNo())
                .setInitiator(userInfo == null ? "" : userInfo.getPersonName())
                .setStartTime(completeTaskPo.getStartTime())
                .setTaskId(completeTaskPo.getId().toString())
                .setTaskName(completeTaskPo.getTaskName())
                .setEndTime(completeTaskPo.getEndTime())
                .setRevoke(Boolean.valueOf(revoke))
                .setProcessId(completeTaskPo.getProcessId())
                .setReject(completeTaskPo.getReject().intValue() == Constants.ENABLED)
                .setShowlog(Boolean.valueOf(showlog))
                .build();
    } 
    
    private List<PendingTaskListResponseVO> voListTransfer(List<PendingTaskPO> taskPos) {
        List<PendingTaskListResponseVO> voList = new LinkedList<>();
        Set<String> ids = taskPos.stream().map(PendingTaskPO::getUserId).map(u -> u.toString()).collect(Collectors.toSet());
        Map<Long, UserDetailDTO> userMap = userServiceAdapter.batchQueryUserById(ids);
        for (PendingTaskPO taskPo : taskPos) {
            UserDetailDTO userDetail = userMap.get(new Long(taskPo.getInitiatorId()));
            PendingTaskListResponseVO taskVo = new PendingTaskListResponseVO.Builder()
                    .setProcessName(taskPo.getProcessName())
                    .setFormNo(taskPo.getTableNo())
                    .setInitiator(userDetail == null ? null : userDetail.getPersonName())
                    .setStartTime(taskPo.getStartTime())
                    .setStatus(taskPo.getTaskStatus())
                    .setTaskId(taskPo.getId().toString())
                    .setTaskName(taskPo.getTaskDescriptionZhCn())
                    .setType(taskPo.getTaskType())
                    .setUrl(taskPo.getOpenUrl())
                    .setProcessId(taskPo.getProcessId())
                    .build();
            voList.add(taskVo);
        }
        return voList;
    }
    /**
     * 委托
     * @param taskId
     * @param mandatary 受托者
     * @param reason 委托原因
     */
    public void entrust(String taskId, String mandatary, String reason) {
        UserOrgDetailDTO mandataryUser = userServiceAdapter.getUserByName(mandatary);
        if (mandataryUser == null) {
            throw new NotExistException(FlowErrorEnum.USER_NOT_EXIST_ERROR);
        }
        Long principal = UserContext.getUserContext().getUserId();
        taskCenterService.entrust(taskId, reason, mandataryUser.getId(), principal);
    }
    
}
