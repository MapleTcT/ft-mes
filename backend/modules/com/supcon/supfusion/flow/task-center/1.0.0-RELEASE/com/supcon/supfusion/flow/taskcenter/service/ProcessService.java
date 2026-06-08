/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.flow.common.dto.AssigneeDTO;
import com.supcon.supfusion.flow.common.dto.AuditDTO;
import com.supcon.supfusion.flow.common.dto.CandidateGroupDTO;
import com.supcon.supfusion.flow.common.dto.DiagramDTO;
import com.supcon.supfusion.flow.common.dto.NotificationDTO;
import com.supcon.supfusion.flow.common.dto.ProcessQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.ProcessStartDTO;
import com.supcon.supfusion.flow.common.dto.SimpleTaskDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.NotificationTopicEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessLogTypeEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.exception.PermissionException;
import com.supcon.supfusion.flow.common.exception.ProcessOperateException;
import com.supcon.supfusion.flow.common.exception.StatusAbnormalException;
import com.supcon.supfusion.flow.common.po.CompleteTaskPO;
import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.po.EntrustPO;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.po.ProcessAttentionPO;
import com.supcon.supfusion.flow.common.po.ProcessLogPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;
import com.supcon.supfusion.flow.common.po.TaskFormPO;
import com.supcon.supfusion.flow.common.util.CodeGenerator;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.MapUtils;
import com.supcon.supfusion.flow.common.util.ProxyUtils;
import com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.AssigneeVO;
import com.supcon.supfusion.flow.common.vo.webapi.AuditVO;
import com.supcon.supfusion.flow.common.vo.webapi.FlowChartResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.PendingTaskResponseVO2;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessLogVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessStartInfoVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProtocolVO;
import com.supcon.supfusion.flow.common.vo.webapi.SimpleUserVO;
import com.supcon.supfusion.flow.common.vo.webapi.UrgeDetailVO;
import com.supcon.supfusion.flow.common.vo.webapi.UrgeInfoVO;
import com.supcon.supfusion.flow.dao.DiagramMapper;
import com.supcon.supfusion.flow.dao.EntrustMapper;
import com.supcon.supfusion.flow.dao.ProcessAttentionMapper;
import com.supcon.supfusion.flow.dao.ProcessLogMapper;
import com.supcon.supfusion.flow.dao.ProcessMapper;
import com.supcon.supfusion.flow.dao.TaskFormMapper;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.job.LoggerJob;
import com.supcon.supfusion.flow.taskcenter.job.NotificationJob;
import com.supcon.supfusion.flow.taskcenter.mybatis.ProcessQueryWrapper;
import com.supcon.supfusion.flow.taskcenter.rpc.NotificationService;
import com.supcon.supfusion.flow.taskcenter.rpc.OrganizationServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;

/**
 * @author: zhuangmh
 * @date: 2020年5月27日 下午2:22:46
 */
@Service
public class ProcessService {

    @Autowired
    private OrganizationServiceAdapter orgService;
    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private DiagramMapper diagramMapper;
    @Autowired
    private ProcessLogMapper processLogMapper;
    @Autowired
    private EntrustMapper entrustMapper;
    @Autowired
    private ProcessAttentionMapper processAttentionMapper;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private TaskCenterService taskCenterService;
    @Autowired
    private ProcessEngineService processEngineService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationJob notificationJob;
    @Autowired
    private LoggerJob loggerJob;
    @Autowired
    private TaskFormMapper taskFormMapper;

    /**
     * 查询我的发起
     * 
     * @param queryContract
     * @param pagination
     * @return
     */
    public PageResult<ProcessVO> queryMy(ProcessQueryContractDTO queryContract, Pagination pagination) {
        String tenantId = RpcContext.getContext().getTenantId();
        Long userId = UserContext.getUserContext().getUserId();
        LambdaQueryWrapper<ProcessPO> processQuery = ProcessQueryWrapper.buildMyProcessQueryWrapper(queryContract, userId, tenantId);
        Integer total = processMapper.selectCount(processQuery);
        processQuery.orderByDesc(ProcessPO::getCreateTime);
        Page<ProcessPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<ProcessPO> processPages = processMapper.selectPage(page, processQuery);
        List<String> processIds = processPages.getRecords().stream().map(ProcessPO::getId).map(p -> p.toString()).collect(Collectors.toList());
        if (!processIds.isEmpty()) {
            Map<String, List<PendingTaskPO>> processMap = taskCenterService.listTaskByProcess(processIds);
            return new PageResult<>(reconstructMyProcess(processPages.getRecords(), processMap), total, pagination.getPageSize(), pagination.getCurrent());
        }
        return new PageResult<>(new ArrayList<>(1), 0, pagination.getPageSize(), pagination.getCurrent());
    }

    /**
     * 查询所有流程
     * 
     * @param queryContract
     * @param pagination
     * @return
     */
    public PageResult<ProcessVO> queryAll(ProcessQueryContractDTO queryContract, Pagination pagination) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<ProcessPO> processQuery = ProcessQueryWrapper.buildProcessQueryWrapper(queryContract, tenantId);
        Integer total = processMapper.selectCount(processQuery);
        processQuery.orderByDesc(ProcessPO::getCreateTime);
        Page<ProcessPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<ProcessPO> processPages = processMapper.selectPage(page, processQuery);
        List<String> processIds = processPages.getRecords().stream().map(ProcessPO::getId).map(p -> p.toString()).collect(Collectors.toList());
        if (!processIds.isEmpty()) {
            Map<String, List<PendingTaskPO>> processMap = taskCenterService.listTaskByProcess(processIds);
            return new PageResult<>(reconstructAllProcess(processPages.getRecords(), processMap), total, pagination.getPageSize(), pagination.getCurrent());
        }
        return new PageResult<>(new ArrayList<>(1), 0, pagination.getPageSize(), pagination.getCurrent());
    }

    private List<ProcessVO> reconstructAllProcess(List<ProcessPO> records, Map<String, List<PendingTaskPO>> processMap) {
        List<ProcessVO> processList = new LinkedList<>();
        for (ProcessPO record : records) {
            List<PendingTaskResponseVO2> taskVOs = new ArrayList<>();
            ProcessVO process = new ProcessVO();
            List<PendingTaskPO> tasks = processMap.get(record.getId().toString());
            if (tasks != null) {
                for (PendingTaskPO t : tasks) {
                    PendingTaskResponseVO2 taskVO = new PendingTaskResponseVO2.Builder().setTaskId(t.getId().toString())
                            .setTaskName(t.getTaskDescriptionZhCn()).setStartTime(t.getStartTime()).setAssignee(t.getPersonName()).build();
                    taskVOs.add(taskVO);
                }
            }
            process.setAppId(record.getAppId());
            process.setProcessId(record.getId().toString());
            process.setVersion(record.getProcessVersion());
            process.setProcessName(record.getProcessName());
            process.setStartTime(record.getCreateTime());
            process.setStatus(record.getProcessStatus());
            process.setInitiator(record.getStaffName());
            process.setTasks(taskVOs);
            processList.add(process);
        }
        return processList;
    }

    private List<ProcessVO> reconstructAttentionProcess(List<ProcessPO> records) {
        List<ProcessVO> processList = new LinkedList<>();
        for (ProcessPO record : records) {
            List<PendingTaskPO> tasks = taskCenterService.getProcessProgress(record.getId().toString());
            List<PendingTaskResponseVO2> taskVOs = new ArrayList<>();
            ProcessVO process = new ProcessVO();
            Set<String> taskNames = tasks.stream().map(PendingTaskPO::getTaskDescriptionZhCn).collect(Collectors.toSet());
            for (String name : taskNames) {
                PendingTaskResponseVO2 taskVO = new PendingTaskResponseVO2.Builder().setTaskName(name).build();
                taskVOs.add(taskVO);
            }
            if (!tasks.isEmpty()) {
                List<TaskFormPO> formDatas = taskFormMapper.selectList(new QueryWrapper<TaskFormPO>().lambda()
                                .eq(TaskFormPO::getProcessId, record.getId())
                                .orderByDesc(TaskFormPO::getCreateTime));
                process.setFormData(formDatas.isEmpty() ? null : formDatas.get(0).getFormData());
            }

            process.setAppId(record.getAppId());
            process.setProcessId(record.getId().toString());
            process.setVersion(record.getProcessVersion());
            process.setProcessName(record.getProcessName());
            process.setStartTime(record.getCreateTime());
            process.setStatus(record.getProcessStatus());
            process.setInitiator(record.getStaffName());
            process.setFormNo(record.getTableNo());
            process.setShowlog(true);
            process.setTasks(taskVOs);
            processList.add(process);
        }
        return processList;
    }

    private List<ProcessVO> reconstructMyProcess(List<ProcessPO> records, Map<String, List<PendingTaskPO>> processMap) {
        List<ProcessVO> processList = new LinkedList<>();
        for (ProcessPO record : records) {
            List<PendingTaskResponseVO2> taskVOs = new ArrayList<>();
            ProcessVO process = new ProcessVO();
            List<PendingTaskPO> tasks = processMap.get(record.getId().toString());
            if (tasks != null && !tasks.isEmpty()) {
                Set<String> taskNames = tasks.stream().map(PendingTaskPO::getTaskDescriptionZhCn).collect(Collectors.toSet());
                for (String name : taskNames) {
                    PendingTaskResponseVO2 taskVO = new PendingTaskResponseVO2.Builder().setTaskName(name).build();
                    taskVOs.add(taskVO);
                }
                List<TaskFormPO> formDatas = taskFormMapper.selectList(new QueryWrapper<TaskFormPO>().lambda()
                                .eq(TaskFormPO::getProcessId, record.getId())
                                .orderByDesc(TaskFormPO::getCreateTime));
                process.setFormData(formDatas.isEmpty() ? null : formDatas.get(0).getFormData());
            }
            process.setAppId(record.getAppId());
            process.setProcessId(record.getId().toString());
            process.setProcessName(record.getProcessName());
            process.setStartTime(record.getCreateTime());
            process.setStatus(record.getProcessStatus());
            process.setFormNo(record.getTableNo());
            process.setShowlog(true);
            process.setTasks(taskVOs);
            processList.add(process);
        }
        return processList;
    }

    /**
     * 查询我的关注列表
     * 
     * @param pagination
     * @return
     */
    public PageResult<ProcessVO> queryMyAttention(String appId, Pagination pagination) {
        Long userId = UserContext.getUserContext().getUserId();
        LambdaQueryWrapper<ProcessAttentionPO> queryWrapper = Wrappers.<ProcessAttentionPO>lambdaQuery();
        queryWrapper.eq(ProcessAttentionPO::getUserId, userId);
        if (appId != null) {
            queryWrapper.eq(ProcessAttentionPO::getAppId, appId);
        }
        Integer total = processAttentionMapper.selectCount(queryWrapper);
        if (total == null || total.intValue() == 0) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        queryWrapper.orderByDesc(ProcessAttentionPO::getCreateTime);
        Page<ProcessAttentionPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<ProcessAttentionPO> pageResult = processAttentionMapper.selectPage(page, queryWrapper);
        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), total, pagination.getPageSize(), pagination.getCurrent());
        }
        List<String> processIds = pageResult.getRecords().stream().map(ProcessAttentionPO::getProcessId).collect(Collectors.toList());
        List<ProcessPO> processes = processMapper.selectBatchIds(processIds);
        return new PageResult<>(reconstructAttentionProcess(processes), total, pagination.getPageSize(), pagination.getCurrent());
    }

    /**
     * 关注流程
     * 
     * @param processId
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void follow(String processId) {
        ProcessPO process = processMapper.selectById(processId);
        Long userId = UserContext.getUserContext().getUserId();
        ProcessAttentionPO attentionPO = new ProcessAttentionPO();
        attentionPO.setId(CodeGenerator.generateUUID());
        attentionPO.setAppId(process.getAppId());
        attentionPO.setProcessId(processId);
        attentionPO.setUserId(userId);
        attentionPO.setInitiatorId(process.getUserId().toString());
        attentionPO.setStaffName(process.getStaffName());
        attentionPO.setTableNo(process.getTableNo());
        attentionPO.setTenantId(RpcContext.getContext().getTenantId());
        processAttentionMapper.insert(attentionPO);
        PendingTaskPO task = new PendingTaskPO();
        task.setAttention(Constants.ENABLED);
        taskCenterService.updateByProcess(processId, task);
    }

    /**
     * 取消关注
     * 
     * @param processId
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelFollow(String processId) {
        Long userId = UserContext.getUserContext().getUserId();
        LambdaQueryWrapper<ProcessAttentionPO> deleteWrapper = Wrappers.<ProcessAttentionPO>lambdaQuery();
        deleteWrapper.eq(ProcessAttentionPO::getUserId, userId);
        deleteWrapper.eq(ProcessAttentionPO::getProcessId, processId);
        processAttentionMapper.delete(deleteWrapper);
        PendingTaskPO task = new PendingTaskPO();
        task.setAttention(Constants.DISABLED);
        taskCenterService.updateByProcess(processId, task);
    }

    /**
     * 获取流程启动信息
     * @param processKey 流程编号
     * @return
     * @throws DocumentException 
     */
    public ProcessStartInfoVO getProcessStartInfo(String appId, String processKey) throws DocumentException {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                        .eq(DiagramPO::getProcessKey, processKey)
                        .and(i -> i.eq(DiagramPO::getValid, Constants.VALID));
        if (appId != null) {
            queryWrapper.and(i -> i.eq(DiagramPO::getAppId, appId));
        }
        if (StringUtils.isNotEmpty(tenantId)) {
            queryWrapper.eq(DiagramPO::getTenantId, tenantId);
        }
        Integer count = diagramMapper.selectCount(queryWrapper);
        if (count == null || count.intValue() == 0) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        queryWrapper.and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED));
        // 获取当前启用版本
        DiagramPO diagram = diagramMapper.selectOne(queryWrapper);
        if (diagram == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_PUBLISHED_ERROR);
        }
        ProcessStartDTO processStartInfo = processEngineService.getProcessStartInfo(processKey, diagram.getVersion(), tenantId, true);
        return voTransfer(processStartInfo, diagram);
    }

    private ProcessStartInfoVO voTransfer(ProcessStartDTO processStartInfo, DiagramPO diagram) {
        ProcessStartInfoVO psiVo = new ProcessStartInfoVO();
        psiVo.setCompanyId(diagram.getCid().toString());
        psiVo.setMultiCompany(diagram.getMultiCompany().intValue() == Constants.ENABLED);
        psiVo.setProcessName(processStartInfo.getProcessName());
        psiVo.setEnableComment(processStartInfo.getEnableComment());
        psiVo.setEnableSave(processStartInfo.isEnableSave());
        psiVo.setReadOnly(processStartInfo.getReadOnly());
        psiVo.setStartTaskName(processStartInfo.getStartTaskName());
        psiVo.setUrl(processStartInfo.getUrl());
        List<AuditVO> auditBranchs = new LinkedList<>();
        if (processStartInfo.getAudits() != null) {
            for (AuditDTO audit : processStartInfo.getAudits()) {
                AuditVO auditVo = new AuditVO();
                BeanUtils.copyProperties(audit, auditVo);
                auditBranchs.add(auditVo);
            }
        }
        List<AssigneeVO> assignVOs = new LinkedList<>();
        if (processStartInfo.getAssigns() != null) {
            for (AssigneeDTO assign : processStartInfo.getAssigns()) {
                AssigneeVO assigneeVo = new AssigneeVO(assign.getId(), assign.getName(), assign.getTaskDefKey());
                assignVOs.add(assigneeVo);
            }
        }
        psiVo.setAssigns(assignVOs);
        psiVo.setAudits(auditBranchs);
        return psiVo;
    }

    public ProcessPO getProcess(String processId) {
        return processMapper.selectById(processId);
    }

    /**
     * 查询组态对应的流程实例
     * 
     * @param processKey
     * @param version
     * @return
     */
    public List<ProcessPO> queryProcessIds(String processKey, int version) {
        LambdaQueryWrapper<ProcessPO> queryWrapper = new QueryWrapper<ProcessPO>().lambda().eq(ProcessPO::getProcessKey, processKey)
                .eq(ProcessPO::getProcessVersion, version);
        return processMapper.selectList(queryWrapper);
    }

    /**
     * 查询流程操作记录
     * 
     * @param processId
     *            流程实例ID
     * @return
     */
    public List<ProcessLogVO> queryProcessLogs(String processId) {
        List<ProcessLogVO> logVos = new LinkedList<>();
        // 技术公司的待办存在没有processId的数据, 也不需要看流程日志
        if (StringUtils.isEmpty(processId)) {
            return logVos;
        }
        ProcessPO process = processMapper.selectById(processId);
        if (process == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        LambdaQueryWrapper<ProcessLogPO> queryWrapper = Wrappers.<ProcessLogPO>lambdaQuery().eq(ProcessLogPO::getProcessId, processId).orderByDesc(ProcessLogPO::getCreateTime);
        List<ProcessLogPO> logEntities = processLogMapper.selectList(queryWrapper);
        for (ProcessLogPO processLog : logEntities) {
            logVos.add(transferLogPo2Vo(processLog));
        }
        return logVos;
    }

    /**
     * 删除流程日志
     * 
     * @param processIds
     */
    public void deleteProcessLog(List<String> processIds) {
        LambdaQueryWrapper<ProcessLogPO> queryWrapper = new QueryWrapper<ProcessLogPO>().lambda().in(ProcessLogPO::getProcessId,
                processIds);
        processLogMapper.delete(queryWrapper);
    }

    /**
     * 删除流程实例
     * 
     * @param processIds
     */
    public void deleteProcess(List<String> processIds) {
        LambdaQueryWrapper<ProcessPO> queryWrapper = new QueryWrapper<ProcessPO>().lambda().in(ProcessPO::getId, processIds);
        processMapper.delete(queryWrapper);
    }

    /**
     * 删除关注的流程
     * 
     * @param processIds
     */
    public void deleteAttentionProcess(List<String> processIds) {
        LambdaQueryWrapper<ProcessAttentionPO> queryWrapper = new QueryWrapper<ProcessAttentionPO>().lambda()
                .in(ProcessAttentionPO::getProcessId, processIds);
        processAttentionMapper.delete(queryWrapper);
    }

    private ProcessLogVO transferLogPo2Vo(ProcessLogPO processLog) {
        ProcessLogVO logVO = new ProcessLogVO();
        logVO.setAuditResult(processLog.getAuditResult());
        logVO.setComment(processLog.getLeaveComment());
        logVO.setCreateTime(processLog.getCreateTime());
        // TODO 国际化
        logVO.setOperateDesc(processLog.getActionDesc());
        logVO.setTaskName(processLog.getTaskName());
        logVO.setOperator(processLog.getCreator());
        logVO.setType(processLog.getActionType());
        return logVO;
    }

    /**
     * 暂存流程待发起, 仅作用于发起流程页面
     * 
     * @param processKey
     *            流程编码
     * @param formData
     *            表单JSON数据
     * @return
     * @throws DocumentException
     */
    public SimpleTaskDTO startProcessBySave(String appId, String processKey, String formData) throws DocumentException {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getProcessKey, processKey)
                .and(i -> i.eq(DiagramPO::getValid, Constants.VALID));
        if (StringUtils.isNotEmpty(appId)) {
            queryWrapper.and(i -> i.eq(DiagramPO::getAppId, appId));
        }
        if (StringUtils.isNotEmpty(tenantId)) {
            queryWrapper.eq(DiagramPO::getTenantId, tenantId);
        }
        Integer count = diagramMapper.selectCount(queryWrapper);
        if (count == null || count.intValue() == 0) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        queryWrapper.and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED));
        // 获取当前启用版本
        DiagramPO diagram = diagramMapper.selectOne(queryWrapper);
        if (diagram == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_PUBLISHED_ERROR);
        }
        ProcessStartDTO startInfo = processEngineService.getProcessStartInfo(diagram.getProcessKey(), diagram.getVersion(), tenantId, false);
        // 判定条件: 开始环节指向一个人工节点, 并且该人工节点的执行者为发起人
        if (!startInfo.isEnableSave()) {
            throw new ProcessOperateException(FlowErrorEnum.PROCESS_NOT_ALLOWED_SAVE_ERROR);
        }
        String userId = UserContext.getUserContext().getUserId().toString();
        Map<String, Object> transientVariables = MapUtils.jsonToMap(formData);
        transientVariables.put(Constants.FORM_DATA, formData);
        transientVariables.put(Constants.ENABLE_DELETE, Constants.ENABLED);
        String processId = processEngineService.startUp(diagram.getProcessKey(), diagram.getVersion(), userId, tenantId, new HashMap<>(),
                transientVariables);
        // 理论上只有一个待办任务
        List<PendingTaskPO> tasks = taskCenterService.listPendingTaskByProcess(processId, false);
        return new SimpleTaskDTO(tasks.get(0).getId().toString(), processId);
    }

    /**
     * 根据流程编号启动流程
     * 
     * @param diagramDto
     *            流程信息
     * @param formData
     *            表单JSON数据
     * @param comment
     *            备注
     * @param assignees
     *            指派者 在拦截器中设置到上下文
     *            {@link com.supcon.supfusion.flow.taskcenter.interceptor.OperationInterceptor}
     * @param auditRequest
     *            分支信息
     * @return
     * @throws DocumentException 
     */
    public String startProcess(DiagramDTO diagramDto, String formData, String comment, List<AssigneeRequestVO> assignees, AuditVO auditRequest)
            throws DocumentException {
        String tenantId = RpcContext.getContext().getTenantId();
        // 查询启用版本的流程
        DiagramPO diagram = getEnableDiagram(diagramDto.getProcessKey(), diagramDto.getAppId(), tenantId);
        if (diagram == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_PUBLISHED_ERROR);
        }
        // 验证提交分支是否存在
        ProcessStartDTO startInfo = processEngineService.getProcessStartInfo(diagramDto.getProcessKey(), diagram.getVersion(), tenantId, true);
        if (startInfo.getAudits() != null && startInfo.getAudits().size() > 1) {
            if (auditRequest == null) {
                throw new NotExistException(FlowErrorEnum.AUDIT_BRANCH_REQUIRED);
            } else {
                boolean sequenceExist = false;
                for (AuditDTO audit : startInfo.getAudits()) {
                    if ((sequenceExist = audit.getId().equals(auditRequest.getId()))) {
                        auditRequest.setValue(audit.getValue());
                        auditRequest.setName(audit.getName());
                        auditRequest.setOrder(audit.getOrder());
                        auditRequest.setType(audit.getType());
                        break;
                    }
                }
                if (!sequenceExist) {
                    throw new NotExistException(FlowErrorEnum.SEQUENCE_NOT_EXIST);
                }
            }
        }
        Set<CandidateGroupDTO> candidateGroups = bpmnService.queryUserTaskCandidateGroup(diagramDto.getProcessKey(), diagram.getVersion());
        if (!candidateGroups.isEmpty()) {
            orgService.validateEmptyMemberTeam(candidateGroups);
        }
        Map<String, Object> transientVariables = MapUtils.jsonToMap(formData);
        transientVariables.put(Constants.FORM_DATA, formData);
        Map<String, Object> variables = buildPersistentVariables(diagramDto.getAppId(), diagramDto.getProcessName());
        return ProxyUtils.getProxyObject(ProcessService.class).doStartWithTransaction(diagram, auditRequest, assignees, variables, transientVariables, comment);
    }

    private DiagramPO getEnableDiagram(String processkey, String appId, String tenantId) {
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getProcessKey, processkey)
                .and(i -> i.eq(DiagramPO::getValid, Constants.VALID));
        if (appId != null) {
            queryWrapper.and(i -> i.eq(DiagramPO::getAppId, appId));
        }
        if (StringUtils.isNotEmpty(tenantId)) {
            queryWrapper.eq(DiagramPO::getTenantId, tenantId);
        }
        Integer count = diagramMapper.selectCount(queryWrapper);
        if (count == null || count.intValue() == 0) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        queryWrapper.and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED));
        // 获取当前启用版本
        return diagramMapper.selectOne(queryWrapper);
    }

    // 方案待定
    /*
     * private boolean hasPermission(String diagramCode) {
     * QueryWrapper<DiagramPermissionPO> queryWrapper = new QueryWrapper<>();
     * queryWrapper.eq(Constants.COL_DIAGRAM_CODE, diagramCode);
     * List<DiagramPermissionPO> permissions =
     * diagramPermissionMapper.selectList(queryWrapper); if (permissions.isEmpty())
     * { return true; } Long staffId = UserContext.getUserContext().getStaffId();
     * PersonDTO person = orgService.getPerson(staffId); for (DiagramPermissionPO
     * permission : permissions) { if ("all".equals(permission.getStaffId()) ||
     * staffId.toString().equals(permission.getStaffId())) { return true; } if
     * (permission.getRoleId() != null) {
     * 
     * } if (permission.getPositionId() != null) {
     * 
     * } if (permission.getDepartmentId() != null) {
     * 
     * } } return false; }
     */

    private Map<String, Object> buildPersistentVariables(String appId, String processName) {
        Map<String, Object> persistentVariables = new HashMap<>();
        persistentVariables.put(Constants.PROCESS_NAME, processName);
        persistentVariables.put(Constants.COL_APPID, appId);
        return persistentVariables;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String doStartWithTransaction(DiagramPO diagram, AuditVO audit, List<AssigneeRequestVO> assignees, Map<String, Object> variables,
            Map<String, Object> transientVariables, String comment) {
        String tenantId = RpcContext.getContext().getTenantId();
        Long userId = UserContext.getUserContext().getUserId();
        String processId = processEngineService.startUp(diagram.getProcessKey(), diagram.getVersion(), userId.toString(), tenantId,
                variables, transientVariables);
        // 判断是否要自动提交待办
        List<PendingTaskPO> tasks = taskCenterService.listPendingTaskByProcess(processId, false);
        if (taskCenterService.detectAutoSubmit(tasks)) {
            // 验证通过, 表示tasks长度为1
            String formData = (String) transientVariables.get(Constants.FORM_DATA);
            taskCenterService.submit(tasks.get(0).getId(), formData, comment, assignees, audit);
        } else {
            notificationService.sendNoticeIfConfigure(processId);
        }
        return processId;
    }

    /**
     * 暂停流程
     * 
     * @param processId
     *            流程实例ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void suspendProcess(String processId) {
        processEngineService.suspendProcess(processId);
        ProcessPO processPO = new ProcessPO();
        processPO.setId(Long.parseLong(processId));
        processPO.setProcessStatus(ProcessStatusEnum.SUSPENDED.getStatus());
        processMapper.updateById(processPO);
        taskCenterService.suspendTask(processId);
        // 记录日志
        loggerJob.submit(createProcessLogPO(processId, Boolean.TRUE));
    }

    private ProcessLogPO createProcessLogPO(String processId, boolean suspend) {
        String userName = UserContext.getUserContext().getUserName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        // TODO 国际化
        if (suspend) {
            processLog.setActionType(ProcessLogTypeEnum.PROCESS_SUSPEND.name());
            processLog.setActionDesc("经由 管理员 暂停流程");
        } else {
            processLog.setActionType(ProcessLogTypeEnum.PROCESS_RESUME.name());
            processLog.setActionDesc("经由 管理员 恢复流程");
        }
        processLog.setProcessId(processId);
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(userName);
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return processLog;
    }

    /**
     * 恢复流程
     * 
     * @param processId
     *            流程实例ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void activeProcess(String processId) {
        processEngineService.activeProcess(processId);
        taskCenterService.activeTask(processId);
        ProcessPO process = new ProcessPO();
        process.setId(Long.parseLong(processId));
        process.setProcessStatus(ProcessStatusEnum.ACTIVED.getStatus());
        processMapper.updateById(process);
        // 记录日志
        loggerJob.submit(createProcessLogPO(processId, Boolean.FALSE));
    }

    /**
     * 催办
     * 
     * @param processId
     *            流程实例ID
     * @param urgeList
     *            催办信息
     * @param protocols
     *            通知方式
     */
    public void urge(String processId, List<UrgeDetailVO> urgeList, List<String> protocols) {
        ProcessStatusEnum processStatus = processEngineService.getProcessStatus(processId);
        if (processStatus == ProcessStatusEnum.SUSPENDED) {
            throw new StatusAbnormalException(FlowErrorEnum.PROCESS_SUSPENDED_ERROR);
        }
        for (UrgeDetailVO urge : urgeList) {
            PendingTaskPO task = taskCenterService.getById(Long.parseLong(urge.getTaskId()));
            Set<Long> userIds = urge.getUserIds().stream().map(Long::parseLong).collect(Collectors.toSet());
            notificationJob.submit(new NotificationDTO(NotificationTopicEnum.TASK_URGE, Collections.singletonList(task), userIds, protocols));
        }
    }

    /**
     * 获取当前流程催办信息
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
                users.add(new SimpleUserVO(pendingTask.getUserId().toString(), pendingTask.getPersonName()));
            }
            urgeInfo.setUsers(users);
            urgeInfos.add(urgeInfo);
        }
        return urgeInfos;
    }

    /**
     * 作废流程
     * 
     * @param processId
     *            流程实例ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancel(String processId, boolean isAdmin) {
        Long userId = UserContext.getUserContext().getUserId();
        ProcessPO process = processMapper.selectById(processId);
        if (process == null) {
            throw new NotExistException(FlowErrorEnum.PROCESS_NOT_EXIST_ERROR);
        }
        if ((process.getUserId().longValue() != userId.longValue()) && !isAdmin) {
           throw new PermissionException(FlowErrorEnum.TASK_NO_SUBMIT_PERMISSION_ERROR);
        }
        processEngineService.terminateProcess(processId);
        ProcessPO processEntity = new ProcessPO();
        processEntity.setId(Long.parseLong(processId));
        processEntity.setProcessStatus(ProcessStatusEnum.CANCELLED.getStatus());
        processMapper.updateById(processEntity);
        processAttentionMapper.delete(Wrappers.<ProcessAttentionPO>lambdaQuery().eq(ProcessAttentionPO::getProcessId, processId));
        // 删除委托记录
        entrustMapper.delete(new QueryWrapper<EntrustPO>().lambda().eq(EntrustPO::getProcessId, processId));
        // 记录操作日志
        loggerJob.submit(createProcessLogPO(processId));
    }

    private ProcessLogPO createProcessLogPO(String processId) {
        String userName = UserContext.getUserContext().getUserName();
        ProcessLogPO processLog = new ProcessLogPO();
        processLog.setId(CodeGenerator.generateUUID());
        // TODO 国际化
        processLog.setActionDesc("经由 管理员 作废流程");
        processLog.setProcessId(processId);
        processLog.setTenantId(RpcContext.getContext().getTenantId());
        processLog.setCreator(userName);
        processLog.setCreateStaffId(UserContext.getUserContext().getStaffId());
        processLog.setActionType(ProcessLogTypeEnum.PROCESS_TERMINATE.name());
        return processLog;
    }

    /**
     * 生成流程图数据
     * 
     * @param processKey
     *            流程编号
     * @param processId
     *            流程实例ID
     * @return
     */
    public FlowChartResponseVO generateDiagram(String processKey, String processId) {
        String tenantId = RpcContext.getContext().getTenantId();
        FlowChartResponseVO diagramVO = new FlowChartResponseVO();
        if (processKey != null) {
            String enabledDiagramJson = diagramMapper.selectEnabledDiagram(processKey, tenantId);
            diagramVO.setJson(enabledDiagramJson);
            return diagramVO;
        }
        ProcessPO processInstance = processMapper.selectById(processId);
        List<PendingTaskPO> pendingTasks = taskCenterService.listPendingTaskByProcess(processId, false);
        List<CompleteTaskPO> completedTasks = taskCenterService.listCompletedTaskByProcess(processId);
        // 已办中去掉进行中的任务
        Iterator<CompleteTaskPO> iterator = completedTasks.iterator();
        while (iterator.hasNext()) {
            CompleteTaskPO next = iterator.next();
            pendingTasks.forEach(pt -> {
                if (next.getId().equals(pt.getId())) {
                    iterator.remove();
                }
            });
        }
        // 根据节点ID进行归类
        Map<String, List<PendingTaskPO>> activeGroups = pendingTasks.stream()
                .collect(Collectors.groupingBy(PendingTaskPO::getActivityName));
        Map<String, List<CompleteTaskPO>> completeGroups = completedTasks.stream()
                .collect(Collectors.groupingBy(CompleteTaskPO::getActivityName));
        diagramVO.setActiveKeys(activeGroups.keySet());
        DiagramPO diagram = diagramMapper.selectSingle(processInstance.getProcessKey(), processInstance.getProcessVersion(), tenantId);
        if (diagram == null) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        diagramVO.setJson(diagram.getPublishedJson());
        List<FlowChartResponseVO.NodeInfo> nodes = new LinkedList<>();
        for (Map.Entry<String, List<CompleteTaskPO>> entry : completeGroups.entrySet()) {
            FlowChartResponseVO.NodeInfo nodeInfo = diagramVO.new NodeInfo();
            nodeInfo.setTaskDefKey(entry.getKey());
            Set<String> recipients = entry.getValue().stream().map(CompleteTaskPO::getPersonName).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            nodeInfo.setRecipient(String.join(",", recipients));
            nodes.add(nodeInfo);
        }
        for (Map.Entry<String, List<PendingTaskPO>> entry : activeGroups.entrySet()) {
            FlowChartResponseVO.NodeInfo nodeInfo = diagramVO.new NodeInfo();
            nodeInfo.setTaskDefKey(entry.getKey());
            String recipient = entry.getValue().stream().map(PendingTaskPO::getPersonName).filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            nodeInfo.setRecipient(recipient);
            nodes.add(nodeInfo);
        }
        diagramVO.setNodeInfo(nodes);
        return diagramVO;
    }
}
