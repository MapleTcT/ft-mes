/**
 * Licensed to the Deep Blue SUPCON
 * @author: zhuangmh
 * @date: 2020年5月19日 上午9:03:40
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.supcon.supfusion.flow.common.dto.DeploymentDTO;
import com.supcon.supfusion.flow.common.dto.DiagramQueryContractDTO;
import com.supcon.supfusion.flow.common.enumeration.DiagramStatusEnum;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.exception.ImportExportException;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.exception.StatusAbnormalException;
import com.supcon.supfusion.flow.common.po.DiagramContentPO;
import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;
import com.supcon.supfusion.flow.common.util.CodeGenerator;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.ProxyUtils;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramExportResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramListWrapper;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProtocolVO;
import com.supcon.supfusion.flow.dao.DiagramContentMapper;
import com.supcon.supfusion.flow.dao.DiagramMapper;
import com.supcon.supfusion.flow.engine.server.service.DeployService;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.mybatis.DiagramQueryWrapper;
import com.supcon.supfusion.flow.taskcenter.mybatis.DiagramUpdateWrapper;
import com.supcon.supfusion.flow.taskcenter.rpc.NotificationService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 上午9:03:40
 */
@Service
public class DiagramService {
    
    @Autowired
    private DiagramMapper diagramMapper;
    @Autowired
    private TaskCenterService taskCenterService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private FormService formService;
    @Autowired
    private DeployService deployService;
    @Autowired
    private ProcessEngineService processEngineService;
    @Autowired
    private DiagramContentMapper diagramContentMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private PolyphagiaService polyphagiaService;
    /**
     * 查询流程列表
     * @param queryContract
     * @return
     */
    public PageResult<DiagramResponseVO> queryDiagrams(DiagramQueryContractDTO queryContract, Pagination pagination) {
        LambdaQueryWrapper<DiagramPO> listQueryWrapper = DiagramQueryWrapper.buildListQueryWrapper(queryContract);
        Page<DiagramPO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Integer total = diagramMapper.selectCount(listQueryWrapper);
        listQueryWrapper.orderByAsc(DiagramPO::getProcessKey).orderByDesc(DiagramPO::getLatestModifyTime);
        Page<DiagramPO> diagramPagenation = diagramMapper.selectPage(page, listQueryWrapper);
        return new PageResult<>(reconstructDiagrams(diagramPagenation.getRecords())
                , total, diagramPagenation.getSize(), diagramPagenation.getCurrent());
    }
    
    private List<DiagramResponseVO> reconstructDiagrams(List<DiagramPO> diagrams) {
        List<DiagramResponseVO> list = new LinkedList<>();
        for (DiagramPO diagram : diagrams) {
            DiagramResponseVO diagramResponse = new DiagramResponseVO.Builder()
                    .setEnable(diagram.getEnabled() == Constants.ENABLED)
                    .setMultiCompany(diagram.getMultiCompany() == Constants.ENABLED)
                    .setProcessKey(diagram.getProcessKey())
                    .setProcessName(diagram.getProcessName())
                    .setStatus(diagram.getProcessStatus())
                    .setVersion(diagram.getVersion())
                    .setId(diagram.getId().toString())
                    .setCreator(diagram.getCreatorStaff())
                    .setLatestModifyTime(diagram.getLatestModifyTime())
                    .setPublishTime(diagram.getPublishTime())
                    .setPublisher(diagram.getPublisher())
                    .build();
            list.add(diagramResponse);
        }
        return list;
    }
    
    /**
     * 查询当前租户所有的流程版本
     * @return
     */
    public PageResult<Integer> queryAllVersion() {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<DiagramPO> queryWrapper = new QueryWrapper<DiagramPO>().lambda()
                .eq(DiagramPO::getTenantId, tenantId)
                .orderByAsc(DiagramPO::getVersion);
        List<DiagramPO> diagrams = diagramMapper.selectList(queryWrapper);
        Set<Integer> versions = diagrams.stream().map(DiagramPO::getVersion).collect(Collectors.toSet());
        return new PageResult<>(versions, 0, 0, 0);
    }
    
    /**
     * 根据ID获取流程详情
     * @param id 记录ID
     * @return
     */
    public DiagramResponseVO getById(long id) {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        List<ProtocolDTO> protocols = notificationService.retrieveProtocols(Constants.TASK_RECEIVE_TOPIC);
        return reconstructDiagram(diagramDb, protocols);
    }
    
    private DiagramResponseVO reconstructDiagram(DiagramPO diagramDb, List<ProtocolDTO> protocols) {
        List<ProtocolVO> protocolsVO = new LinkedList<>();
        for (ProtocolDTO protocol : protocols) {
            protocolsVO.add(new ProtocolVO(protocol.getProtocol(), protocol.getName()));
        }
        return new DiagramResponseVO.Builder()
                .setCompanyId(diagramDb.getCid().toString())
                .setMultiCompany(diagramDb.getMultiCompany() == Constants.ENABLED)
                .setProcessKey(diagramDb.getProcessKey())
                .setProcessName(diagramDb.getProcessName())
                .setStatus(diagramDb.getProcessStatus())
                .setVersion(diagramDb.getVersion())
                .setId(diagramDb.getId().toString())
                .setJson(diagramDb.getDraftJson())
                .setEnable(diagramDb.getEnabled() == Constants.ENABLED)
                .setProtocols(protocolsVO)
                .build();
    }
    
    /**
     * 新建流程
     * @param name 流程名称 
     * @param appId
     * @return 流程编号和记录ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public DiagramResponseVO createDiagram(String appId, String name, Boolean multiCompany) {
        // 创建PO实体
        DiagramContentPO diagramContentPo = createDiagramContentPo(null);
        DiagramPO diagramPo = createDiagramPo(appId, name, diagramContentPo.getId(), multiCompany);
        diagramContentMapper.insert(diagramContentPo);
        diagramMapper.insert(diagramPo);
        return new DiagramResponseVO.Builder()
                .setId(diagramPo.getId().toString())
                .setProcessKey(diagramPo.getProcessKey())
                .build();
    }
    
    /**
     * @return
     */
    private DiagramContentPO createDiagramContentPo(String draftJson) {
        DiagramContentPO diagramContentPo = new DiagramContentPO();
        diagramContentPo.setId(CodeGenerator.generateUUID());
        diagramContentPo.setDraftJson(draftJson);
        diagramContentPo.setCreator(UserContext.getUserContext().getUserName());
        diagramContentPo.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return diagramContentPo;
    }

    private DiagramPO createDiagramPo(String appId, String name, Long contentId, Boolean multiCompany) {
        Long staffId = UserContext.getUserContext().getStaffId();
        String staffName = UserContext.getUserContext().getStaffName();
        String userName = UserContext.getUserContext().getUserName();
        Long companyId = UserContext.getUserContext().getCompanyId();
        DiagramPO diagramPo = new DiagramPO();
        diagramPo.setProcessName(name);
        diagramPo.setAppId(appId);
        diagramPo.setContentId(contentId);
        diagramPo.setCid(companyId);
        diagramPo.setMultiCompany(multiCompany != null && multiCompany ? Constants.ENABLED : Constants.DISABLED);
        diagramPo.setProcessKey(CodeGenerator.generateProcessKey());
        diagramPo.setProcessStatus(DiagramStatusEnum.CREATION.getStatus());
        diagramPo.setVersion(1);
        diagramPo.setEnabled(Constants.DISABLED);
        diagramPo.setId(CodeGenerator.generateUUID());
        diagramPo.setStartOnMobile(Constants.ENABLED);
        diagramPo.setTenantId(RpcContext.getContext().getTenantId());
        diagramPo.setCreatorStaff(staffName);
        diagramPo.setCreateStaffId(staffId);
        diagramPo.setCreator(userName);
        return diagramPo;
    }

    /**
     * 编辑流程信息
     * @param id 
     * @param name 流程名称
     * @param multiCompany 是否支持跨公司
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void editDiagram(long id, String name, Boolean multiCompany) {
        DiagramPO diagramDb = diagramMapper.selectOne(DiagramQueryWrapper.buildIdQueryWrapper(id));
        if (diagramDb == null) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        DiagramPO updateDiagram = reconstructDiagramPo(diagramDb, name, multiCompany);
        diagramMapper.updateById(updateDiagram);
        // 如果已经发布的流程名称改了, 需要更新待办的流程名称
        if (!diagramDb.getProcessName().equals(name)
                && diagramDb.getProcessStatus() != DiagramStatusEnum.DRAFT.getStatus()) {
            polyphagiaService.syncChangeProcessName(diagramDb.getProcessKey(), diagramDb.getVersion(), name);
        }
    }
    
    /**
     * 更新流程组态数据
     * @param id 记录ID
     * @param diagramJson 组态数据JSON
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveDiagramJson(long id, String diagramJson) {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        // 创建PO实体
        DiagramPO diagramPo = reconstructDiagramPo(id, diagramDb.getProcessStatus());
        DiagramContentPO diagramContentPo = reconstructDiagramContentPo(diagramDb.getContentId(), diagramJson);
        diagramMapper.updateById(diagramPo);
        diagramContentMapper.updateById(diagramContentPo);
    }
    
    private DiagramPO reconstructDiagramPo(DiagramPO diagramDb,  String name, Boolean multiCompany) {
        DiagramPO diagramPO = new DiagramPO();
        diagramPO.setId(diagramDb.getId());
        if (diagramDb.getProcessStatus() == DiagramStatusEnum.CREATION.getStatus()) {
            diagramPO.setMultiCompany(multiCompany != null && multiCompany ? Constants.ENABLED : Constants.DISABLED);
        }
        diagramPO.setProcessName(name);
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        String formatDate = sdf.format(new Date());
        diagramPO.setLatestModifyTime(formatDate);
        return diagramPO;
    }
    
    private DiagramPO reconstructDiagramPo(long id, int statusInDb) {
        DiagramPO diagramPO = new DiagramPO();
        diagramPO.setId(id);
        if (statusInDb == DiagramStatusEnum.PUBLISHED.getStatus()) {
            diagramPO.setProcessStatus(DiagramStatusEnum.DRAFT.getStatus());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        String formatDate = sdf.format(new Date());
        diagramPO.setLatestModifyTime(formatDate);
        return diagramPO;
    }
    
    private DiagramContentPO reconstructDiagramContentPo(long contentId, String diagramJson) {
        DiagramContentPO diagramContentPo = new DiagramContentPO();
        diagramContentPo.setId(contentId);
        diagramContentPo.setDraftJson(diagramJson);
        return diagramContentPo;
    }
    
    private boolean recordNonExist(DiagramPO diagramDb) {
        return diagramDb == null;
    }
    
    /**
     * 发布流程
     * @param id 记录ID
     * @param bpmnXml 流程引擎bpmn数据格式
     */
    public void publish(long id, String bpmnXml) throws UnsupportedEncodingException, DocumentException {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        String tenantId = RpcContext.getContext().getTenantId();
        DiagramStatusEnum currentStatus = DiagramStatusEnum.getByStatus(diagramDb.getProcessStatus());
        switch (currentStatus) {
            case CREATION: { // 全新发布
                ProxyUtils.getProxyObject(DiagramService.class).newReleasePublish(diagramDb, bpmnXml);
                break; 
            }
            case DRAFT: { // 更新发布
                DeploymentDTO currentDeployment = deployService.getLatestDeployment(diagramDb.getProcessKey(), diagramDb.getVersion(), tenantId);
                int newVersion = ProxyUtils.getProxyObject(DiagramService.class).updateReleasePublish(diagramDb, bpmnXml);
                if (currentDeployment != null) {
                    processEngineService.migrateProcessInstanceWithTenant(currentDeployment.getProcessDefinitionId(), newVersion, tenantId);
                }
                break;
            }
            case PUBLISHED: break;
            default: throw new StatusAbnormalException(FlowErrorEnum.DIAGRAM_STATUS_NOT_ALLOW_PUBLISH_ERROR);
        }
    }
    
    /**
     * 该方法只在2.7->3.0升级过程会被调用, 负责运行期老数据迁移
     * @param id
     * @param bpmnXml
     * @throws DocumentException
     * @throws UnsupportedEncodingException
     */
    public void upgradePublish(long id, String bpmnXml) throws DocumentException, UnsupportedEncodingException {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        DeploymentDTO currentDeployment = deployService.getLatestDeployment(diagramDb.getProcessKey(), diagramDb.getVersion(), "dt");
        int newVersion = ProxyUtils.getProxyObject(DiagramService.class).updateReleasePublish(diagramDb, bpmnXml);
        if (currentDeployment != null) {
            processEngineService.migrateProcessInstanceWithTenant(currentDeployment.getProcessDefinitionId(), newVersion, "dt");
        }
        DiagramContentPO diagramContent = new DiagramContentPO();
        diagramContent.setId(diagramDb.getContentId());
        diagramContent.setXml(bpmnXml);
        diagramContentMapper.updateById(diagramContent);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void newReleasePublish(DiagramPO diagramDb, String bpmnXml) throws UnsupportedEncodingException, DocumentException {
        String tenantId = RpcContext.getContext().getTenantId();
        deployService.deployByTenantId(diagramDb.getProcessKey(), diagramDb.getVersion(), bpmnXml, tenantId);
        updateDiagram(diagramDb, bpmnXml);
        enableDiagram(diagramDb.getId()); // 将当前版本设置为启用
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public int updateReleasePublish(DiagramPO diagramDb, String bpmnXml) throws UnsupportedEncodingException, DocumentException {
        String tenantId = RpcContext.getContext().getTenantId();
        DeploymentDTO deployDto = deployService.deployByTenantId(diagramDb.getProcessKey(), diagramDb.getVersion(), bpmnXml, tenantId);
        updateDiagram(diagramDb, bpmnXml);
        return deployDto.getProcessDefinitionVersion();
    }

    private void updateDiagram(DiagramPO diagramDb, String newBpmnXml) {
        DiagramPO newDiagramPo = new DiagramPO();
        newDiagramPo.setId(diagramDb.getId());
        newDiagramPo.setProcessStatus(DiagramStatusEnum.PUBLISHED.getStatus());
        newDiagramPo.setPublisher(UserContext.getUserContext().getStaffName());
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        String formatDate = sdf.format(new Date());
        newDiagramPo.setPublishTime(formatDate);
        DiagramContentPO diagramContentPo = new DiagramContentPO();
        diagramContentPo.setId(diagramDb.getContentId());
        diagramContentPo.setPublishedJson(diagramDb.getDraftJson());
        diagramContentPo.setXml(newBpmnXml);
        diagramMapper.updateById(newDiagramPo);
        diagramContentMapper.updateById(diagramContentPo);
    }
    
    /**
     * 版本升级
     * @param id 记录ID
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public DiagramResponseVO upgradeDiagram(long id, String processName, boolean multiCompany) {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        Integer maxVersion = diagramMapper.selectMaxVersion(diagramDb.getAppId(), diagramDb.getProcessKey(), RpcContext.getContext().getTenantId());
        DiagramContentPO diagramContentPo = createDiagramContentPo(diagramDb.getDraftJson());
        DiagramPO newDiagramDb = reconstructDiagramPo(diagramDb, maxVersion, diagramContentPo.getId(), processName, multiCompany);
        diagramMapper.insert(newDiagramDb);
        diagramContentMapper.insert(diagramContentPo);
        return new DiagramResponseVO.Builder()
                .setId(newDiagramDb.getId().toString())
                .build();
    }
    
    private DiagramPO reconstructDiagramPo(DiagramPO diagramDb, int maxVersion, Long contentId, String processName, boolean multiCompany) {
        Long companyId = UserContext.getUserContext().getCompanyId();
        DiagramPO newDiagramDb = new DiagramPO();
        newDiagramDb.setProcessName(processName);
        newDiagramDb.setAppId(diagramDb.getAppId());
        newDiagramDb.setCid(companyId);
        newDiagramDb.setMultiCompany(multiCompany ? Constants.ENABLED : Constants.DISABLED);
        newDiagramDb.setProcessStatus(DiagramStatusEnum.CREATION.getStatus());
        newDiagramDb.setEnabled(Constants.DISABLED);
        newDiagramDb.setId(CodeGenerator.generateUUID());
        newDiagramDb.setStartOnMobile(Constants.ENABLED);
        newDiagramDb.setTenantId(RpcContext.getContext().getTenantId());
        newDiagramDb.setProcessKey(diagramDb.getProcessKey());
        newDiagramDb.setVersion(++maxVersion);
        newDiagramDb.setContentId(contentId);
        newDiagramDb.setCreator(UserContext.getUserContext().getUserName());
        newDiagramDb.setCreatorStaff(UserContext.getUserContext().getStaffName());
        newDiagramDb.setCreateStaffId(UserContext.getUserContext().getStaffId());
        return newDiagramDb;
    }
    
    /**
     * 启用并设置为当前版本
     * @param id 记录ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enableDiagram(long id) {
        DiagramPO diagramDb = diagramMapper.selectOne(DiagramQueryWrapper.buildIdQueryWrapper(id));
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        if (diagramDb.getEnabled() == Constants.ENABLED) {
            throw new StatusAbnormalException(FlowErrorEnum.DIAGRAM_ENABLE_DUPLICATE_ERROR);
        }
        // 该流程的其他版本置为停用
        DiagramPO newDiagramDb = new DiagramPO();
        newDiagramDb.setEnabled(Constants.DISABLED);
        diagramMapper.update(newDiagramDb, DiagramUpdateWrapper.buildEnableWrapper(diagramDb.getAppId(), diagramDb.getProcessKey(), RpcContext.getContext().getTenantId()));
        // 启用当前版本
        newDiagramDb.setId(id);
        newDiagramDb.setEnabled(Constants.ENABLED);
        diagramMapper.updateById(newDiagramDb);
        // 设置启动权限,移动端启用权限
        // setPermissions(diagramDb);
    }
    
    // 方案待定
    /*private void setPermissions(DiagramPO diagramDb) {
        DiagramContentPO diagramContent = diagramContentMapper.selectById(diagramDb.getContentId());
        String permissionCongig = DomUtils.getAttributeValue(diagramContent.getXml(), "//uri:process/startEvent[@permission]", Constants.START_PERMISSION);
        if (StringUtils.isEmpty(permissionCongig)) {
            return;
        }
        String mobileStartConfig = DomUtils.getAttributeValue(diagramContent.getXml(), "//uri:process/startEvent[@startOnMobile]", Constants.MOBILE_START);
        Integer mobileStart = Boolean.valueOf(mobileStartConfig) ? Constants.ENABLED : Constants.DISABLED;
        String[] permissions = permissionCongig.split(Constants.SPLIT_COMMA);
        // 删除之前的权限设置
        QueryWrapper<DiagramPermissionPO> wrapper = new QueryWrapper<>();
        wrapper.eq(Constants.COL_DIAGRAM_CODE, diagramDb.getProcessKey());
        diagramPermissionMapper.delete(wrapper);
        for (String permission : permissions) {
            DiagramPermissionPO permissionPo = new DiagramPermissionPO();
            permissionPo.setId(CodeGenerator.generateUUID());
            permissionPo.setMobileStart(mobileStart);
            permissionPo.setProcessKey(diagramDb.getProcessKey());
            permissionPo.setProcessName(diagramDb.getProcessName());
            // 所有人都可以发起流程
            if (Constants.PERMISSION_ALL.equals(permission)) {
                permissionPo.setStaffId(Constants.PERMISSION_ALL);
                diagramPermissionMapper.insert(permissionPo);
                break;
            }
            if (permission.startsWith(Constants.PERSON_PREFIX)) {
                String personId = permission.substring(Constants.PERSON_PREFIX.length());
                permissionPo.setStaffId(personId);
            } else if (permission.startsWith(Constants.POS_PREFIX)) {
                String positionId = permission.substring(Constants.POS_PREFIX.length());
                permissionPo.setPositionId(Long.parseLong(positionId));
            } else if (permission.startsWith(Constants.DEPT_PREFIX)) {
                String departmentId = permission.substring(Constants.DEPT_PREFIX.length());
                permissionPo.setDepartmentId(Long.parseLong(departmentId));
            } else if (permission.startsWith(Constants.ROLE_PREFIX)) {
                String roleId = permission.substring(Constants.ROLE_PREFIX.length());
                permissionPo.setRoleId(Long.parseLong(roleId));
            }
            diagramPermissionMapper.insert(permissionPo);
        }
        
    }*/
    
    /**
     * 停用当前版本
     * @param id 记录ID
     */
    public void disableDiagram(long id) {
        DiagramPO newDiagramDb = new DiagramPO();
        newDiagramDb.setId(id);
        newDiagramDb.setEnabled(Constants.DISABLED);
        int result = diagramMapper.updateById(newDiagramDb);
        if (recordNonExist(result)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
    }
    
    private boolean recordNonExist(int result) {
        return result <= 0;
    }

    /**
     * 重置组态数据,由草稿变为未修改状态
     * @param id 记录ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void resetDiagram(long id) {
        DiagramPO diagramDb = diagramMapper.selectSingleById(id);
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        DiagramPO diagram = new DiagramPO();
        diagram.setId(id);
        diagram.setProcessStatus(DiagramStatusEnum.PUBLISHED.getStatus());
        DiagramContentPO diagramContent = new DiagramContentPO();
        diagramContent.setId(diagramDb.getContentId());
        diagramContent.setDraftJson(diagramDb.getPublishedJson());
        diagramMapper.updateById(diagram);
        diagramContentMapper.updateById(diagramContent);
    }

    /**
     * 删除流程组态 
     * @param id 记录ID 多个id用逗号隔开
     * @param onlyDiagram 只删除流程组态数据
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteDiagram(long id, boolean onlyDiagram) {
        DiagramPO diagramDb = diagramMapper.selectOne(DiagramQueryWrapper.buildIdQueryWrapper(id));
        if (recordNonExist(diagramDb)) {
            throw new NotExistException(FlowErrorEnum.DIAGRAM_NOT_EXIST_ERROR);
        }
        diagramMapper.deleteById(id);
        if (onlyDiagram) {
            return;
        }
        List<ProcessPO> processes = processService.queryProcessIds(diagramDb.getProcessKey(), diagramDb.getVersion());
        if (!processes.isEmpty()) {
            List<String> processIds = processes.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
            processService.deleteProcess(processIds);
            processService.deleteProcessLog(processIds);
            processService.deleteAttentionProcess(processIds);
            taskCenterService.deletePendingTask(processIds);
            taskCenterService.deleteCompleteTask(processIds);
            taskCenterService.deleteEntrust(processIds);
            formService.deleteForm(processIds);
            // 删除引擎流程实例
            List<String> activeProcessIds = processes.stream()
                    .filter(p -> p.getProcessStatus().intValue() == ProcessStatusEnum.ACTIVED.getStatus() 
                                || p.getProcessStatus().intValue() == ProcessStatusEnum.SUSPENDED.getStatus())
                    .map(p -> p.getId().toString())
                    .collect(Collectors.toList());
            List<String> completeProcessIds = processes.stream()
                    .filter(p -> p.getProcessStatus().intValue() == ProcessStatusEnum.COMPLETED.getStatus())
                    .map(p -> p.getId().toString())
                    .collect(Collectors.toList());
            processEngineService.batchDeleteProcessInstances(activeProcessIds, completeProcessIds);
        }
    }
    
    /**
     * 批量删除
     * @param ids
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void batchDeleteDiagram(String ids, boolean onlyDiagram) {
        String[] split = ids.split(Constants.SPLIT_COMMA);
        for (String id : split) {
            deleteDiagram(Long.parseLong(id), onlyDiagram);
        }
    }

   
    /**
     *  工作流流程组态数据导出	
     * @param appId
     * @param processKeyVersions  结构为: [processKey_version]
     * @return
     */
    public DiagramListWrapper exports(String appId, List<String> processKeyVersions) {
        String tenantId = RpcContext.getContext().getTenantId();
        List<DiagramExportResponseVO> exportResults = new ArrayList<>();
        for (String processKeyVersion : processKeyVersions) {
            String[] split = processKeyVersion.split(Constants.UNDERLINE);
            if (split.length == 2) {
                DiagramPO diagram = diagramMapper.selectSingle(split[0], Integer.parseInt(split[1]), tenantId);
                if (diagram != null) {
                    DiagramExportResponseVO diagramResponse = new DiagramExportResponseVO();
                    diagramResponse.setAppId(appId);
                    diagramResponse.setCompanyId(diagram.getCid().toString());
                    diagramResponse.setCreator(diagram.getCreator());
                    diagramResponse.setCreateStaffId(diagram.getCreateStaffId());
                    diagramResponse.setJson(diagram.getDraftJson());
                    diagramResponse.setMultiCompany(diagram.getMultiCompany());
                    diagramResponse.setProcessKey(diagram.getProcessKey());
                    diagramResponse.setProcessName(diagram.getProcessName());
                    diagramResponse.setVersion(diagram.getVersion());
                    exportResults.add(diagramResponse);
                }
            }
        }
        return new DiagramListWrapper(appId, exportResults);
    }

    /**
     * 
     * @param diagramJson
     */
    @Transactional
    public void imports(String appId, String diagramJson) {
        DiagramListWrapper diagramList = new Gson().fromJson(diagramJson, DiagramListWrapper.class);
        if (diagramList == null 
                || diagramList.getList() == null 
                || diagramList.getList().isEmpty()) {
            throw new ImportExportException(FlowErrorEnum.DIAGRAM_IMPORT_ERROR);
        }
        if (!diagramList.getAppId().equals(appId)) {
            throw new ImportExportException(FlowErrorEnum.DIAGRAM_CROSS_APP_IMPORT_ERROR);
        }
        Long cid = UserContext.getUserContext().getCompanyId();
        String username = UserContext.getUserContext().getUserName();
        String staffName = UserContext.getUserContext().getStaffName();
        String tenantId = RpcContext.getContext().getTenantId();
        for (DiagramExportResponseVO diagramVO : diagramList.getList()) {
            DiagramPO diagram = new DiagramPO();
            DiagramContentPO dc = new DiagramContentPO();
            dc.setId(CodeGenerator.generateUUID());
            dc.setDraftJson(diagramVO.getJson());
            Integer maxVersion = diagramMapper.selectMaxVersion(appId, diagramVO.getProcessKey(), tenantId);
            int varsion = maxVersion == null ? 1 : maxVersion.intValue() + 1;
            diagram.setVersion(varsion);
            diagram.setCid(cid == null ? 0 : cid);
            diagram.setId(CodeGenerator.generateUUID());
            diagram.setEnabled(0);
            diagram.setProcessStatus(DiagramStatusEnum.CREATION.getStatus());
            diagram.setContentId(dc.getId());
            diagram.setAppId(appId);
            diagram.setProcessKey(diagramVO.getProcessKey());
            diagram.setProcessName(diagramVO.getProcessName());
            diagram.setTenantId(tenantId);
            diagram.setCreator(username);
            diagram.setCreatorStaff(staffName);
            diagram.setMultiCompany(diagramVO.getMultiCompany());
            diagramMapper.insert(diagram);
            diagramContentMapper.insert(dc);
        }
    }
    
}
