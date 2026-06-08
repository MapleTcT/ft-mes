package com.supcon.supfusion.configuration.workflow.service.impl;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.entity.DealInfoType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.dao.DeploymentDaoImpl;
import com.supcon.supfusion.base.dao.PendingDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.entity.DealInfo;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.EntityTableInfo;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.workflow.dao.CountersignAssignStaffDaoImpl;
import com.supcon.supfusion.configuration.workflow.dao.ExpectedConsignDaoImpl;
import com.supcon.supfusion.configuration.workflow.dao.FlowCurrentStatusDaoImpl;
import com.supcon.supfusion.configuration.workflow.dao.TaskDaoImpl;
import com.supcon.supfusion.configuration.workflow.entities.CountersignAssignStaff;
import com.supcon.supfusion.configuration.workflow.entities.ExpectedConsign;
import com.supcon.supfusion.configuration.workflow.entities.FlowCurrentStatus;
import com.supcon.supfusion.configuration.workflow.script.TextExecutor;
import com.supcon.supfusion.configuration.workflow.service.EntityTableInfoService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowProcessService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import com.supcon.supfusion.configuration.workflow.variables.Variables;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.jbpm.api.Execution;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessEngine;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.cmd.Command;
import org.jbpm.api.cmd.Environment;
import org.jbpm.api.model.Activity;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.env.ExecutionContext;
import org.jbpm.pvm.internal.id.DbidGenerator;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/25
 */
@Slf4j
@Service
public class WorkflowTaskServiceImpl extends BaseServiceImpl implements WorkflowTaskService {

    @Autowired
    private TaskDaoImpl taskDao;
    @Autowired
    private PendingDaoImpl pendingDao;
    @Autowired
    private DeploymentDaoImpl deploymentDao;
    @Autowired
    private ExpectedConsignDaoImpl expectedConsignDao;
    @Autowired
    private EntityTableInfoService entityTableInfoService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExecutionService executionService;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private StaffService staffService;

    private ThreadLocal<Boolean> recall_recallAbleFlag = new ThreadLocal<>();
    private ThreadLocal<String> recall_lastTaskName = new ThreadLocal<>();
    private ThreadLocal<String> recall_sourceStaff = new ThreadLocal<>();
    private ThreadLocal<ProcessInstance> processInstance = new ThreadLocal<>();
    private ThreadLocal<String> autoCreateDealInfos = new ThreadLocal<>();

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Task getTask(String code, Long deploymentId) {
        List<Task> list = taskDao.findByCriteria(Restrictions.eq("code", code), Restrictions.eq("deploymentId", deploymentId));
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<ExpectedConsign> getExpectedConsignList(String flowKey, String activeCode, int type) {
        return null;
    }

    @Transactional
    public void expectedConsignRecallSchedule(List<ExpectedConsign> list,int type) {
        if(list != null && list.size() > 0){
            for(ExpectedConsign ec : list){
                if(type==1 || type==3){
                    ec.setValid(false);
                }else if(type==2){
                    ec.setRecallFlag(true);
                }
                expectedConsignDao.save(ec);
            }
            expectedConsignRevoke(list, type);
        }
    }

    @Override
    public void save(Task task) {
        taskDao.save(task);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void expectedConsignRevoke(List<ExpectedConsign> list,int type) {
        if(list != null && list.size() > 0){
            for(ExpectedConsign ec : list){
                expectedConsignRevoke(ec, type);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void expectedConsignRevoke(ExpectedConsign expectedConsign,int type) {
        if (expectedConsign != null) {
            List<Pending> pendingList = new ArrayList<Pending>();
            if ("ALL".equals(expectedConsign.getType())) {

                pendingList = pendingDao.findByCriteria(
                        Restrictions.isNotNull("sourceStaff"),
                        Restrictions.like("sourceStaff",
                                "%" + expectedConsign.getUserId() + "%"),
                        Restrictions.between("createTime",
                                expectedConsign.getStartDate(),
                                expectedConsign.getEndDate()));
            } else {
                pendingList = pendingDao.findByCriteria(
                        Restrictions.eq("processKey",
                                expectedConsign.getFlowKey()),
                        Restrictions.isNotNull("sourceStaff"),
                        Restrictions.like("sourceStaff",
                                "%" + expectedConsign.getUserId() + "%"),
                        Restrictions.eq("activityName",
                                expectedConsign.getActiveCode()),
                        Restrictions.between("createTime",
                                expectedConsign.getStartDate(),
                                expectedConsign.getEndDate()));
            }
            generateEcDealInfo(pendingList, expectedConsign.getUserId(), type);
            expectedConsignRevokePendings(pendingList,
                    expectedConsign.getUserId());

        }
    }

    private void generateEcDealInfo(List<Pending> pendingList, Long userId, int type) {
        if(pendingList != null && pendingList.size() > 0){
            final List<DealInfo> dealInfoList = new ArrayList<DealInfo>();
            for(Pending p : pendingList){
                String sourceStaffStr=p.getSourceStaff();
                if(sourceStaffStr==null || sourceStaffStr.length()==0){
                    continue;
                }
                String[] arr=sourceStaffStr.split(",");
                Boolean sflag=false;
                for(String str :arr){
                    if(str.equals(userId.toString())){
                        sflag=true;
                    }
                }
                if (sflag) {
                    Deployment deployment = deploymentDao.load(p
                            .getDeploymentId());
                    EntityTableInfo entityTable = (EntityTableInfo) entityTableInfoService
                            .getITableInfo(p.getTableInfoId());
                    String targetTable = entityTable.getTargetTableName();
                    DealInfo di = new DealInfo();
                    di.setActivityName(p.getActivityName());
                    di.setCreateTime(new Date());
                    di.setDealInfoType(DealInfoType.EXPECTEDCONSIGNRECALL);
                    di.setEntityCode(deployment.getEntityCode());
                    di.setProcessKey(deployment.getProcessKey());
                    di.setProcessVersion(deployment.getProcessVersion());
                    di.setTableInfoId(p.getTableInfoId());
                    if (getCurrentUser() != null) {
                        di.setUserId(getCurrentUser().getId());
                    } else {
                        di.setUserId(1000L);// 系统管理员
                    }
                    User user = userService.load(p.getUserId());
                    if(type == 3){
                        di.setTaskDescription("由于活动变更为不允许委托而撤回");
                        di.setOutcomeDes("由于活动变更为不允许委托而从" + user.getStaff().getName()
                                + "处收回预期委托待办");
                    }else{
                        di.setTaskDescription("撤回");
                        di.setOutcomeDes("从" + user.getStaff().getName()
                                + "处收回预期委托待办");
                    }
                    dealInfoList.add(di);

                    String sql = "INSERT INTO "
                            + DbUtils.getDealInfoTable(targetTable)
                            + "(ID,CREATE_TIME,TABLE_INFO_ID,TASK_DESCRIPTION,USER_ID,VERSION,DEALINFO_TYPE,ENTITY_CODE,INSTANCE_ID,OUTCOME_DES,PROCESS_KEY,PROCESS_VERSION,ACTIVITY_NAME) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    long entityDealInfoId = DbidGenerator.getNextId(null, 1, null);
                    List<Object> params = new ArrayList<Object>();
                    params.add(entityDealInfoId);
                    params.add(new java.sql.Timestamp(System
                            .currentTimeMillis()));
                    params.add(di.getTableInfoId());
                    params.add(di.getTaskDescription());
                    params.add(di.getUserId());
                    params.add(0);
                    params.add(DealInfoType.EXPECTEDCONSIGNRECALL.toString());
                    // params.add(di.getAssignStaff());
                    params.add(di.getEntityCode());
                    params.add(di.getInstanceId());
                    params.add(di.getOutcomeDes());
                    params.add(di.getProcessKey());
                    params.add(di.getProcessVersion());
                    params.add(di.getActivityName());
                    jdbcTemplate.update(sql, params.toArray());
                }
            }
            pendingDao.getSession().doWork(new Work() {
                public void execute(Connection connection) throws SQLException {
                    String sql1 = "INSERT INTO wf_deal_info(ID,CREATE_TIME,TABLE_INFO_ID,TASK_DESCRIPTION,USER_ID,VERSION,DEALINFO_TYPE,ASSIGN_STAFF,ENTITY_CODE,INSTANCE_ID,OUTCOME_DES,PROCESS_KEY,PROCESS_VERSION,ACTIVITY_NAME,TASK_DESCRIPTION_ZH_CN,OUTCOME_DES_ZH_CN) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    PreparedStatement ps = connection.prepareStatement(sql1);
                    long dealInfoId = DbidGenerator.getNextId(null, 1, null);
                    for(DealInfo di : dealInfoList){
                        ps.setLong(1, dealInfoId);
                        ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                        ps.setLong(3, di.getTableInfoId());
                        ps.setString(4, di.getTaskDescription());
                        ps.setLong(5, di.getUserId());
                        ps.setInt(6, 0);
                        ps.setString(7, DealInfoType.EXPECTEDCONSIGNRECALL.toString());
                        ps.setString(8, di.getAssignStaff());
                        ps.setString(9, di.getEntityCode());
                        ps.setString(10, di.getInstanceId());
                        ps.setString(11, di.getOutcomeDes());
                        ps.setString(12, di.getProcessKey());
                        ps.setInt(13, di.getProcessVersion());
                        ps.setString(14, di.getActivityName());
                        ps.setString(15, InternationalResource.get(di.getTaskDescription()));
                        ps.setString(16, InternationalResource.get(di.getOutcomeDes()));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    ps.close();
                }
            });
        }
    }

    private void expectedConsignRevokePendings(List<Pending> pendingList, Long userId) {

        if (pendingList != null && pendingList.size() > 0) {
            for (Pending p : pendingList) {
                String sourceStaffStr = p.getSourceStaff();
                if (sourceStaffStr == null || sourceStaffStr.length() == 0) {
                    continue;
                }
                String[] arr = sourceStaffStr.split(",");
                Boolean sflag = false;
                for (String str : arr) {// 判断待办的sourceStaff中是否包含改预期委托中的委托人
                    if (str.equals(userId.toString())) {
                        sflag = true;
                    }
                }

                if (sflag) {
                    if (arr.length == 1) {
                        if (p.getTaskType() == 0) {// 自身有权限
                            if (!checkExistPending(p, userId)) {
                                Pending np = p;
                                // 判断是否是会签，如果是会签需要生成引擎的分支
                                if ("countersign".equals(p
                                        .getActivityType())) {
                                    final ExecutionImpl execution = (ExecutionImpl) executionService
                                            .findExecutionById(p
                                                    .getExecutionId());
                                    createCountersingPro(execution, userId, np);
                                }
                                np.setId(null);
                                np.setUserId(userId);
                                np.setSourceStaff(null);
                                np.setProxySource(null);
                                np.setTaskType(0);
                                pendingDao.save(np);
                            }
                            p.setSourceStaff(null);
                            pendingDao.save(p);
                        } else if (p.getTaskType() == 2) {// 自身没权限
                            if (!checkExistPending(p, userId)) {
                                p.setUserId(userId);
                                p.setSourceStaff(null);
                                p.setProxySource(null);
                                p.setTaskType(0);
                                pendingDao.save(p);
                            } else {
                                if (checkExistOtherPending(p)) {//判断是否是唯一待办
                                    pendingDao.delete(p);
                                }
                            }
                        }
                    } else {
                        String[] idArr = p.getSourceStaff().split(",");
                        List<String> idList = new ArrayList<String>();
                        for (String id : idArr) {
                            if (!id.equals(userId.toString())) {
                                idList.add(id);
                            }
                        }
                        // 修改待办的sourceStaff
                        String staffIdStr = Arrays.toString(idList.toArray())
                                .substring(
                                        1,
                                        Arrays.toString(idList.toArray())
                                                .length() - 1);
                        p.setSourceStaff(staffIdStr);
                        pendingDao.save(p);
                        if (!checkExistPending(p, userId)) {
                            Pending np = p;
                            // 判断是否是会签，如果是会签需要生成引擎的分支
                            if ("countersign".equals(p
                                    .getActivityType())) {
                                final ExecutionImpl execution = (ExecutionImpl) executionService
                                        .findExecutionById(p.getExecutionId());
                                createCountersingPro(execution, userId, np);
                            }
                            np.setId(null);
                            np.setUserId(userId);
                            np.setSourceStaff(null);
                            np.setProxySource(null);
                            np.setTaskType(0);
                            pendingDao.save(np);
                        }
                    }
                }
            }
        }
    }

    public boolean checkExistPending(Pending p, Long userId) {
        DetachedCriteria dc = DetachedCriteria.forClass(Pending.class);
        dc.add(Restrictions.eq("userId", userId))
                .add(Restrictions.eq("processKey", p.getProcessKey()))
                .add(Restrictions.eq("activityName", p.getActivityName()))
                .add(Restrictions.eq("tableInfoId", p.getTableInfoId()));
        long count = pendingDao.getCountByCriteria(dc);
        if (count > 0L) {
            return true;
        }
        return false;
    }

    public boolean checkExistOtherPending(Pending p) {
        DetachedCriteria dc = DetachedCriteria.forClass(Pending.class);
        dc.add(Restrictions.eq("processKey", p.getProcessKey()))
                .add(Restrictions.eq("activityName", p.getActivityName()))
                .add(Restrictions.eq("tableInfoId", p.getTableInfoId()));
        long count = pendingDao.getCountByCriteria(dc);
        if (count > 1L) {
            return true;
        }
        return false;
    }

    private void createCountersingPro(final ExecutionImpl execution,final Long userId, final Pending np){

        Command command = new Command() {
            private static final long serialVersionUID = 5027277030928314809L;

            @Override
            public Object execute(Environment environment) throws Exception {

                ExecutionImpl concurrentExecution = createCountersignImp(
                        execution, userId, environment);
                np.setExecutionId(concurrentExecution.getId());
                return null;
            }
        };

        processEngine.execute(command);
    }

    private ExecutionImpl createCountersignImp(ExecutionImpl execution,Long userId,Environment environment){

        ExecutionImpl rootExecution = execution.getParent();

        ExecutionImpl concurrentExecution = rootExecution.createExecution(Long.toString(userId));
        concurrentExecution.setTableInfoId(execution.getTableInfoId());
        // concurrentExecution.setEntityId(execution.getEntityId());
        concurrentExecution.setEntityCode(execution.getEntityCode());
        concurrentExecution.setDeploymentId(execution.getDeploymentId());
        concurrentExecution.setInitiatorPositionId(execution.getInitiatorPositionId());
        concurrentExecution.setModelId(execution.getModelId());
        concurrentExecution.setOwnerId(execution.getOwnerId());
        concurrentExecution.setOwnerPositionId(execution.getOwnerPositionId());
        concurrentExecution.setProcessInitiator(execution.getProcessInitiator());
        concurrentExecution.setTableNo(execution.getTableNo());
        concurrentExecution.setTableName(execution.getTableName());
        concurrentExecution.setGroupEnabled(execution.getGroupEnabled());
        concurrentExecution.setVariablesProvider(execution.getVariablesProvider());
        concurrentExecution.setActivity(execution.getActivity());
        concurrentExecution.setGroupEnabled(execution.getGroupEnabled());
        concurrentExecution.setCrossCompanyFlag(execution.getCrossCompanyFlag());
        concurrentExecution.setHistoryActivityStart(new Date());
        concurrentExecution.setScriptExcuteBeanName(execution.getScriptExcuteBeanName());
        concurrentExecution.setState(Execution.STATE_ACTIVE_CONCURRENT);
        concurrentExecution.setTransition(execution.getTransition());
        concurrentExecution.setWorkFlowVar(execution.getWorkFlowVar());
        if (environment != null) {
            ((EnvironmentImpl) environment).setContext(new ExecutionContext(concurrentExecution));
        }
        /*** 生成实例**************start **/
        String transitionName = "";
        if (concurrentExecution.getTransition() != null) {
            transitionName = concurrentExecution.getTransition().getName();
        } else if (concurrentExecution.getWorkFlowVar() != null) {
            transitionName = concurrentExecution.getWorkFlowVar().getOutcome();
        } else {
            transitionName = concurrentExecution.getActivity().getOutgoingTransitions().get(0).getName();
        }
        concurrentExecution.historyActivityStart(transitionName);
        /*** 生成实例**************end **/
        return concurrentExecution;
    }

    @Transactional
    public void dealMobileApprovePending(String processKey, int processVersion, Boolean flowMobileApprove, boolean isMobileApproveChanged) {
        if (isMobileApproveChanged) {
            if (flowMobileApprove != null && flowMobileApprove) {
                String sql = "UPDATE wf_pending p set p.MOBILE_APPROVE = ?0 WHERE p.PROCESS_KEY = ?1 AND EXISTS (SELECT 1 FROM WF_TASK t WHERE t.CODE = p.ACTIVITY_NAME AND t.PROCESS_KEY = p.PROCESS_KEY AND t.PROCESS_VERSION = p.PROCESS_VERSION AND t.MOBILE_APPROVE = ?2 AND t.VALID = 1)";
                pendingDao.createNativeQuery(sql, new Object[] { 1, processKey, 1 }).executeUpdate();
                pendingDao.createNativeQuery(sql, new Object[] { 0, processKey, 0 }).executeUpdate();
            } else {
                String hql = "update Pending p set p.mobileApprove = false where p.processKey = ?0";
                pendingDao.bulkExecute(hql, new Object[] { processKey });
            }
        } else if (flowMobileApprove) {
            String hql = "update Pending p set p.mobileApprove = ?0 where p.processKey = ?1 and p.processVersion = ?2 and p.activityName in (select code from Task t where t.processKey = ?3 and t.processVersion = ?4 and t.mobileApprove = ?5)";
            pendingDao.bulkExecute(hql, new Object[] { true, processKey, processVersion, processKey, processVersion, true });
            pendingDao.bulkExecute(hql, new Object[] { false, processKey, processVersion, processKey, processVersion, false });
        } // else 无需处理
    }

    @Override
    public void setRecallAbleFlag(Boolean bool) {
        // 更改前提：仅当该变量未设置，或者之前设置为true，现在要改为false时
        if(recall_recallAbleFlag.get() == null || !recall_recallAbleFlag.get()) {
            recall_recallAbleFlag.set(bool);
        }
    }

    @Autowired
    private FlowCurrentStatusDaoImpl flowCurrentStatusDao;
    @Override
    @Transactional
    public void deleteFlowCurrentStatus(Execution execution) {
        ExecutionImpl exec = (ExecutionImpl) execution;
        flowCurrentStatusDao.createQuery("delete FlowCurrentStatus where tableInfoId=? and currActivityName = ?", exec.getTableInfoId(),exec.getActivityName()).executeUpdate();
    }

    @Autowired
    private ModelService modelService;
    @Autowired
    private EntityService entityService;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void invalid(OpenExecution execution) {
        pendingDao.createQuery("delete Pending where instanceId = ?", execution.getProcessInstance().getId()).executeUpdate();
        Long tableInfoId = execution.getTableInfoId();
        if (null != tableInfoId) {
            Object[] arr = (Object[])pendingDao.createNativeQuery(
                    "SELECT " + EntityTableInfo.COL_TARGET_TABLE_NAME + ", " + EntityTableInfo.COL_TARGET_ENTITY_CODE + " FROM " + EntityTableInfo.TABLE_NAME + " WHERE ID = ?",
                    tableInfoId).uniqueResult();
            String tableName = (String) arr[0];
            String entityCode = (String) arr[1];
            if (null != tableName) {
                String tableInfoIdColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "tableInfoId", null);
                String statusColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "status", null);
                pendingDao.createNativeQuery("UPDATE " + tableName + " SET " + statusColName + " = ? WHERE " + tableInfoIdColName + " = ?",
                        EntityTableInfo.STATUS_INVALID, tableInfoId).addSynchronizedQuerySpace("").executeUpdate();
            }
            pendingDao.createNativeQuery(
                    "UPDATE " + EntityTableInfo.TABLE_NAME + " SET " + EntityTableInfo.COL_STATUS + " = ? WHERE ID = ?",
                    EntityTableInfo.STATUS_INVALID, tableInfoId).addSynchronizedEntityClass(EntityTableInfo.class).executeUpdate();
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void effect(OpenExecution execution) {
        Long tableInfoId = execution.getTableInfoId();
        if (null != tableInfoId) {
            Object[] arr = (Object[]) pendingDao.createNativeQuery(
                    "SELECT " + EntityTableInfo.COL_TARGET_TABLE_NAME + ", " + EntityTableInfo.COL_TARGET_ENTITY_CODE + " FROM " + EntityTableInfo.TABLE_NAME + " WHERE ID = ?",
                    tableInfoId).uniqueResult();
            String tableName = (String) arr[0];
            String entityCode = (String) arr[1];
            Long effectStaffId = getCurrentStaff().getId();
            if (null != effectStaffId) {
                Date effectTime = new Date();
                if (null != tableName) {
                    String tableInfoIdColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "tableInfoId", null);
                    String effectStaffIdColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "effectStaff", true);
                    String effectTimeColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "effectTime", null);
                    String statusColName = modelService.getPropertyColumnNameByTableName(entityCode, tableName, "status", null);
                    pendingDao.createNativeQuery(
                            "UPDATE " + tableName + " SET " + statusColName + " = ?," + effectStaffIdColName
                                    + " = ?," + effectTimeColName + " = ? WHERE " + tableInfoIdColName + " = ?",
                            EntityTableInfo.STATUS_EFFECTED, effectStaffId, effectTime, tableInfoId).addSynchronizedQuerySpace("").executeUpdate();
                }
                pendingDao.createNativeQuery(
                        "UPDATE " + EntityTableInfo.TABLE_NAME + " SET " + EntityTableInfo.COL_STATUS + " = ?,"
                                + EntityTableInfo.COL_EFFECT_STAFF_ID + " = ?," + EntityTableInfo.COL_EFFECT_TIME + " = ? WHERE ID = ?",
                        EntityTableInfo.STATUS_EFFECTED, effectStaffId, effectTime, tableInfoId).addSynchronizedEntityClass(EntityTableInfo.class).executeUpdate();
            }
        }
    }

    @Override
    @Transactional
    public Deployment getDeployment(Long id) {
        return deploymentDao.load(id);
    }

    @Override
    public boolean getRecallAbleFlag() {
        return (recall_recallAbleFlag.get() == null || recall_recallAbleFlag.get());
    }

    @Override
    public String getLastTaskName() {
        return recall_lastTaskName.get();
    }

    @Override
    @Transactional
    public void saveFlowCurrentStatus(Execution execution) {

        ExecutionImpl exec = (ExecutionImpl) execution;

        FlowCurrentStatus status = new FlowCurrentStatus();
        status.setActivityType(exec.getActivity().getType());
        status.setLastActivityName(getLastTaskName().split("\\|\\|")[1]);
        status.setAutoCreateDealInfos(autoCreateDealInfos.get());
        status.setCurrActivityName(exec.getActivityName());
        if (exec.getTransition() != null) {
            status.setInTransition(exec.getTransition().getName());
        }
        status.setCreateTime(new Date());
        status.setDealer((Staff) getCurrentStaff());
        status.setTableInfoId(exec.getTableInfoId());
        status.setDeploymentId(exec.getDeploymentId());
        status.setSourceStaff(recall_sourceStaff.get());

        flowCurrentStatusDao.save(status);
    }

    @Override
    public boolean checkDeploymentMobileApprove(Deployment d) {
        return d.getMobileapprove() == null ? false : d.getMobileapprove();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void createPendings(final List<Pending> pendings) {
        if (!pendings.isEmpty()) {
            long start = System.currentTimeMillis();
            Long deploymentId = pendings.get(0).getDeploymentId();
            final Deployment d = deploymentDao.load(deploymentId);
            final List<Pending> mQPendings = new ArrayList<Pending>();
            final long id = DbidGenerator.getNextId(Pending.TABLE_NAME, pendings.size() + 1, null);
            long uid = id;

            pendingDao.getSessionFactory().getCurrentSession().doWork(new Work() {
                @Override
                public void execute(Connection conn) throws SQLException {
                    String sql = "INSERT INTO "
                            + Pending.TABLE_NAME
                            + "(USER_ID,TASK_DESCRIPTION,ACTIVITY_TYPE,ACTIVITY_NAME,EXECUTION_ID,STATUS,CREATE_TIME,OPEN_URL,INSTANCE_ID,PROCESS_KEY,"
                            + "PROCESS_VERSION,PROCESS_NAME,PROCESS_ID,PROCESS_DESCRIPTION,TABLE_INFO_ID,ENTITY_CODE,TABLE_NO,DEPLOYMENT_ID,TASK_TYPE,PROXY_SOURCE,"
                            + "DESCRIPTION,LOOPS,ID,VERSION,CID,MODEL_ID,SYSTEM_CALENDAR_ID,MAIN_LOOP,SOURCE_STAFF,MOBILE_APPROVE,DESCRIPTION_ZH_CN,TASK_DESCRIPTION_ZH_CN,PROCESS_DESCRIPTION_ZH_CN) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    long pid = id;
                    for (Pending p : pendings) {
                        ps.setLong(1, p.getUserId());
                        ps.setString(2, p.getTaskDescription());
                        ps.setString(3, p.getActivityType());
                        ps.setString(4, p.getActivityName());
                        ps.setString(5, p.getExecutionId());
                        ps.setObject(6, p.getStatus());
                        // ps.setDate(7, new
                        // java.sql.Date(System.currentTimeMillis()));
                        ps.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
                        ps.setString(8, p.getOpenUrl());
                        ps.setString(9, p.getInstanceId());
                        ps.setString(10, d.getProcessKey());
                        ps.setInt(11, d.getProcessVersion());
                        ps.setString(12, d.getProcessName());
                        ps.setString(13, d.getProcessDefinitionId());
                        ps.setString(14, d.getName());
                        ps.setLong(15, p.getTableInfoId());
                        ps.setString(16, p.getEntityCode());
                        ps.setString(17, p.getTableNo());
                        ps.setLong(18, p.getDeploymentId());
                        ps.setObject(19, p.getTaskType());
                        if (p.getProxySource() != null && p.getProxySource().length() > 0) {
                            ps.setString(20, p.getProxySource());
                        } else {
                            ps.setString(20, null);
                        }
                        ps.setString(21, p.getDescription());
                        ps.setInt(22, (p.getLoop() != null) ? p.getLoop() : 0);

                        ps.setLong(23, ++pid);

                        ps.setInt(24, 0);
                        User user= userService.load(p.getUserId());
                        Staff pendingStaff =staffService.load(user.getStaff().getId());
                        Long cid = getPendingCid(p.getUserId(), p.getEntityCode());
                        if (cid == null) {
                            cid = pendingStaff.getMainPosition().getCompany().getId();
                        }
                        ps.setLong(25, cid);
                        //ps.setLong(25, pendingStaff.getMainPosition().getCid());
                        ps.setLong(26, p.getModelId());
                        ps.setString(27, "");
                        ps.setInt(28, (p.getMainLoop() != null && p.getMainLoop()) ? 1 : 0);
                        if (p.getSourceStaff() != null && p.getSourceStaff().length() > 0) {
                            ps.setString(29, p.getSourceStaff());
                        } else {
                            ps.setString(29, null);
                        }
                        ps.setInt(30, p.getMobileApprove() != null && p.getMobileApprove() ? 1 : 0);
                        ps.setString(31, InternationalResource.get(p.getDescription()));
                        ps.setString(32, InternationalResource.get(p.getTaskDescription()));
                        ps.setString(33, InternationalResource.get(d.getName()));



                        ps.addBatch();
                        if (log.isDebugEnabled())
                            log.debug(sql);
                    }
                    ps.executeBatch();
                    ps.close();
                }
            });
            if (log.isDebugEnabled())
                log.debug("===============生成{}条待办耗时:{}ms", pendings.size(), System.currentTimeMillis() - start);

            //发送待办MQ消息
            for(Pending p : pendings){
                p.setId(++uid);
                p.setProcessDescription(d.getName());
                p.setCreateTime(new Date());
                mQPendings.add(p);
            }
//            sendPendingsToMQ(mQPendings, "task_create");
        }
    }

    private Long getPendingCid(Long userId, String entityCode) {
        Long cid = null;
        if (null != getCurrentUser() && userId.equals(getCurrentUser().getId())) {
            cid = getCurrentCompanyId();
        } else {
            Entity entity = entityService.getEntity(entityCode);
            if (entity.getCrossCompanyFlag() == null || !entity.getCrossCompanyFlag()) {
                cid = getCurrentCompanyId();
            }
        }
        return cid;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<Long, List<Long>> getAssigeUser(OpenExecution execution, Deployment deployment) {
        // List<ExpectedConsign>
        // ecList=expectedConsignDao.findByCriteria(Restrictions.eq("flowKey",
        // flowKey),Restrictions.eq("activeCode", activeCode),
        // Restrictions.eq("valid",true),Restrictions.gt("startDate", new
        // Date()),Restrictions.lt("endDate", new Date()));
        //预期委托，先判断当前活动是否允许委托，若否直接返回null
        Activity active = execution.getActivity();
        Deployment currentDeployment = deployment;
        if(!deployment.getIsCurrentVersion()){
            currentDeployment=getCurrentDeployment(deployment.getProcessKey());
        }
        if(currentDeployment != null){
            Task currentTask = getTask(active.getName(),currentDeployment.getId());
            if(currentTask == null || !currentTask.getIsAllowProxy()){
                return new HashMap<Long, List<Long>>();
            }
        }
        String sql = "select u.ID USERID,ec.CONSIGNOR_ID CONSIGNORID from WF_EXPECTED_CONSIGN ec,BASE_USERINFO u,BASE_USERINFO au "
                + " where ((EC.FLOW_KEY=? and EC.ACTIVE_CODE=?) or EC.TYPE='ALL') and EC.START_DATE<=? and EC.END_DATE>=? "
                + " and EC.USER_ID=u.ID and EC.CONSIGNOR_ID=au.ID and EC.VALID=1 and U.VALID=1 and AU.VALID=1 ";
        NativeQuery query = pendingDao.createNativeQuery(sql, deployment.getProcessKey(), active.getName(), new Date(), new Date());

        @SuppressWarnings("unchecked")
        List<Object[]> ecList = query.list();

        final Map<Long, List<Long>> map = new HashMap<Long, List<Long>>();
        for (Object[] ec : ecList) {
            // Long
            // userId=(ec.get("USERID")!=null)?Long.valueOf(ec.get("USERID").toString()):-1L;
            Long userId = (ec[0] != null) ? Long.valueOf(ec[0].toString()) : null;
            Long consignorId = (ec[1] != null) ? Long.valueOf(ec[1].toString()) : null;
            if (userId == null || consignorId == null) {
                continue;
            }
            if (map.get(userId) == null) {
                map.put(userId, new ArrayList<Long>());
            }
            map.get(userId).add(consignorId);
        }
        return map;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Deployment getCurrentDeployment(String key) {
        List<Deployment> list = deploymentDao.findByCriteria(Restrictions.eq("processKey", key), Restrictions.eq("isCurrentVersion", true));
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;

    }

    @Autowired
    private ProcessService processService;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    protected String getInitiatorGroups(OpenExecution execution) {
        Long tableInfoId = execution.getTableInfoId();
        StringBuilder ids = new StringBuilder();

        try {
            List list = pendingDao.createNativeQuery("SELECT BAP_GROUP_ID FROM " + DbUtils.getGroupTable(execution.getTableName()) + " WHERE TABLE_INFO_ID = ?",
                    tableInfoId).list();
            if (!list.isEmpty()) {
                for (Object o : list) {
                    ids.append(",").append(o);
                }
                ids.deleteCharAt(0);
            }
        } catch (Exception e) {
            throw new EcException(e);
        }
        return ids.toString();
    }

    @Autowired
    private DataPermissionService dataPermissionService;
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public void getUserList(Map<String, Set<Long>> userIds, OpenExecution execution, String inputorFlag, String leaderFlag,
                            String bigLeaderFlag, String staffIds, String flowDealFlag, String activityDealFlag, String attentFlag,
                            Map<Long, String> entrust)  {
        Deployment deployment = processService.getDeployment(execution.getDeploymentId());
        // deployment.gete
        String activeCode = execution.getActivity().getName();
        String processKey = deployment.getProcessKey();
        ActivityImpl active = (ActivityImpl) execution.getActivity();
        if(active.getDescription()==null){
            Task task = getTask(activeCode, deployment.getId());
            active.setDescription(task.getName());
        }
        int processVersion = deployment.getProcessVersion();
        String groupIds = "";
        String tableName = DbUtils.getDealInfoTable(execution.getTableName());
        Long tableNO = execution.getTableInfoId();
        if (execution.getGroupEnabled() != null && execution.getGroupEnabled()){
            groupIds = getInitiatorGroups(execution);
        }
        Boolean crossCompanyFlag = false;
        if (execution.getCrossCompanyFlag() != null && execution.getCrossCompanyFlag()) {
            crossCompanyFlag = true;
        }
        if(deployment.getCrossCompanyFlag()  != null&&deployment.getCrossCompanyFlag() ){
            crossCompanyFlag = true;
        }
        // TODO
        List<Long> userList = dataPermissionService.getPowerUserList(execution.getProcessInitiator(), activeCode, processKey,
                String.valueOf(processVersion), execution.getInitiatorPositionId(), groupIds, crossCompanyFlag);
        if (null == userList || userList.isEmpty()) {
            userList = new ArrayList<Long>();
        }
        User inputUser = userService.load(execution.getProcessInitiator());
        Staff staff;

        if (inputorFlag != null && inputorFlag.equals("true")) {
            if (inputUser != null) {
                userList.add(inputUser.getId());
                // assignorList.add(new AssignUser(inputUser.getName()));
            }
        }
        if (leaderFlag != null && leaderFlag.equals("true")) {
//            staff = inputUser.getStaff();
//            if (staff.getLeaderStaffId() != null) {
//
//                Staff leaderStaff = staffService.load(staff.getLeaderStaffId());
//                if (null != leaderStaff) {
//                    User leaderUser = leaderStaff.getUser();
//                    userList.add(leaderUser.getId());
//                }
//            }
        }
        if (bigLeaderFlag != null && bigLeaderFlag.equals("true")) {
//            staff = inputUser.getStaff();
//            if (staff.getHigherLeaderStaffId() != null) {
//                Staff HigherloaderStaff = staffService.load(staff.getHigherLeaderStaffId());
//                if (HigherloaderStaff != null) {
//                    User higherLeaderUser = HigherloaderStaff.getUser();
//                    if (null != higherLeaderUser) {
//                        userList.add(higherLeaderUser.getId());
//                    }
//                }
//            }
        }
        if (flowDealFlag != null && flowDealFlag.equals("true")) {
            String sql = "select USER_ID from " + tableName + " where TABLE_INFO_ID=?";
            List<Long> flowDealer = jdbcTemplate.queryForList(sql, new Object[] { tableNO }, Long.class);
            if (flowDealer.size() > 0) {
                userList.addAll(flowDealer);
            }

        }
        if (activityDealFlag != null && activityDealFlag.equals("true")) {
            String sql = "select USER_ID from " + tableName + " where TABLE_INFO_ID=?" + " and ACTIVITY_NAME=?";
            List<Long> activityDealer = jdbcTemplate.queryForList(sql, new Object[] { tableNO, activeCode }, Long.class);
            if (activityDealer.size() > 0) {
                userList.addAll(activityDealer);
            }
        }
        if (activityDealFlag != null && activityDealFlag.equals("true")) {
            String sql = "select USER_ID from " + tableName + " where TABLE_INFO_ID=?" + " and ACTIVITY_NAME=?";
            List<Long> activityDealer = jdbcTemplate.queryForList(sql, new Object[] { tableNO, activeCode }, Long.class);
            if (activityDealer.size() > 0) {
                userList.addAll(activityDealer);
            }
        }
        // 关注人
        if (attentFlag != null && attentFlag.equals("true")) {
            String sql = "select s.USER_ID from " + DbUtils.getPayAttentionTable(execution.getTableName())
                    + " pa,BASE_STAFF s where pa.TABLE_INFO_ID=? and pa.valid=1 and pa.staff=s.id";
            List<Long> attention = jdbcTemplate.queryForList(sql, new Object[] { tableNO, }, Long.class);
            if (attention.size() > 0) {
                userList.addAll(attention);
            }
        }
        // 指定人员实现
        // /////////////////////////////////////////////////////////////////
        ExecutionImpl ex = (ExecutionImpl) execution;
        String currentAct = ex.getActivity().getType();
        if (null != ex.getWorkFlowVar()
                && !("decision".equals(currentAct) || "fork".equals(currentAct) || "info".equals(currentAct) || "auto".equals(currentAct))) {

            String outcome = ex.getWorkFlowVar().getOutcome();
            List<TransitionImpl> transitionList = (List<TransitionImpl>) execution.getActivity().getIncomingTransitions();
            for (TransitionImpl imp : transitionList) {
                String type = imp.getSource().getType();

                if (type != null && ("decision".equals(type) || "fork".equals(type) || "info".equals(type) || "auto".equals(type))) {// 如果是分发或选择路由，需要查找上个迁移线
                    List<TransitionImpl> trans = (List<TransitionImpl>) imp.getSource().getIncomingTransitions();
                    for (TransitionImpl ip : trans) {
                        if (ip.getName().equals(outcome)) {// 判断是否是普通主迁移线（为了区分通知线）
                            Set<Long> assignUsers = ex.getWorkFlowVar().getAdditionalUsers();
                            if (null != assignUsers && assignUsers.size() > 0) {
                                userList.addAll(assignUsers);
                            }
                        }
                    }
                } else {
                    if (imp.getName().equals(outcome)) {// 判断是否是普通主迁移线（为了区分通知线）
                        Set<Long> assignUsers = ex.getWorkFlowVar().getAdditionalUsers();
                        if (null != assignUsers && assignUsers.size() > 0) {
                            userList.addAll(assignUsers);
                        }
                    }

                }
            }

        }

        // 候选人
        if (null != staffIds && staffIds.trim().length() > 0) {
            // 先解析出来
            ((ExecutionImpl) execution).setVariablesProvider(((ExecutionImpl) execution).getWorkFlowVar().getVariablesProvider());
            Map<String, Object> variables = Variables.executeAll(execution);
            String[] staffArr = staffIds.split(",");
            for (String staffIdsTemp : staffArr) {
                String evalutatedStaffIds = TextExecutor.execute(staffIdsTemp, variables);
                if (null != evalutatedStaffIds && evalutatedStaffIds.trim().length() > 0) {
                    String[] ss = evalutatedStaffIds.split(",");
                    Long staffId;
                    for (String sId : ss) {
                        try {
                            staffId = Long.valueOf(sId);
                        } catch (NumberFormatException e) {
                            staffId = null;
                        }
                        if (null != staffId) {
                            staff = staffService.get(staffId);
                            if (null != staff) {
                                User user = staff.getUser();
                                if (null != user) {
                                    userList.add(user.getId());

                                }
                            }
                        }
                    }
                }
            }

        }
        Set<Long> normalSet = new HashSet<Long>();
        normalSet.addAll(userList);
        if (userIds.get(Pending.NORMAL_PENDING) != null && userIds.get(Pending.NORMAL_PENDING).size() > 0) {
            normalSet.addAll(userIds.get(Pending.NORMAL_PENDING));
        }
        userIds.put(Pending.NORMAL_PENDING, normalSet);
        //去掉了预期委托的代码，发现跟活动的预期委托重复---------需要验证

        // 获取当前活动的预期委托信息
        // key为委托人，value为被委托人列表
        Map<Long, List<Long>> expectedConsignor = getAssigeUser(execution, deployment);
        Set<Long> temp = new HashSet<Long>();
        for (Long pendingUserId : userList) {
            // 如果当前活动有委托给其他用户
            if (expectedConsignor.get(pendingUserId) != null) {
                temp.addAll(expectedConsignor.get(pendingUserId));
                List<Long> check = new ArrayList<Long>();
                check.add(pendingUserId);
                getUsers(temp, expectedConsignor.get(pendingUserId), expectedConsignor, userList, check);
            }
        }
        if (!temp.isEmpty()) {
            userIds.put(Pending.EXPECTED_PENDING, temp);
        }
        // key为委托人(本次处理生成待办的原始待办所有人)，value为被委托人列表(最终)
        Map<Long, List<Long>> e = new HashMap<Long, List<Long>>();
        List<Long> check = new ArrayList<Long>();
        if (entrust != null) {
            for (Long pendingUserId : userList) {
                if (expectedConsignor.get(pendingUserId) != null) {
                    //e.put(pendingUserId, expectedConsignor.get(pendingUserId));
                    // temp.addAll(expectedConsignor.get(pendingUserId));
                    check.add(pendingUserId);
                    findUserId(pendingUserId, pendingUserId, expectedConsignor, e, check);
                    check.removeAll(check);
                    for (Long id : expectedConsignor.get(pendingUserId)) {
                        if (entrust.get(id) != null && !entrust.get(id).contains(String.valueOf(pendingUserId))) {
                            entrust.put(id, entrust.get(id) + "," + pendingUserId);
                        } else {
                            entrust.put(id, String.valueOf(pendingUserId));
                        }
                        getEntrust(id, expectedConsignor, entrust);
                    }
                }
            }
        }
        generateDealInfo(execution, deployment, e);

        // if (!temp.isEmpty()) {
        // userList.addAll(temp);
        // }

        // userIds.addAll(userList);
    }

    private void findUserId(Long id, Long tempId, Map<Long, List<Long>> assigeUser, Map<Long, List<Long>> relation, List<Long> check){
        if (assigeUser == null) {
            assigeUser = new HashMap<Long, List<Long>>();
        }
        if(assigeUser.get(tempId)!=null){ // 该活动上，当前被处理的用户没有预期委托信息
            List<Long> ids = assigeUser.get(tempId);
            for(Long i : ids){
                if(check.contains(i)){// 出现循环委托的情况
                    StringBuffer sb = new StringBuffer();
                    for(Long d : check){
                        User user = (User) userService.load(d);
                        if(user!=null && user.getStaff()!=null){
                            sb.append(user.getStaff().getName()).append("->");
                        }
                    }
                    User user = (User) userService.load(i);
                    if(user!=null && user.getStaff()!=null){
                        sb.append(user.getStaff().getName());
                    }
                    throw new EcException(EcException.Code.EXPECTED_CONSIGN_CIRCULAR_REFERENCE, sb.toString());
                }else{
                    check.add(i);
                }
                findUserId(id, i, assigeUser, relation, check);
            }
        } else {
            if (relation.get(id) == null && id.longValue() != tempId.longValue()) {
                relation.put(id, new ArrayList<Long>());
            }
            if (relation.get(id) != null && !relation.get(id).contains(tempId)) {
                relation.get(id).add(tempId);
            }
            check.remove(check.size() - 1);
        }
    }

    private void getEntrust(Long i, Map<Long, List<Long>> expectedConsignor, Map<Long, String> entrust) {
        if (expectedConsignor.get(i) != null) {
            for (Long id : expectedConsignor.get(i)) {
                if (entrust.get(id) == null) {
                    entrust.put(id, String.valueOf(i));
                } else if (!entrust.get(id).contains(String.valueOf(i))) {
                    entrust.put(id, entrust.get(id) + "," + i);
                }
                getEntrust(id, expectedConsignor, entrust);
            }
        }
    }

    @Autowired
    private WorkflowProcessService workflowProcessService;
    private void generateDealInfo(Pending pending, Map<Long, List<Long>> map){
        if (!map.isEmpty()) {
            Deployment deployment = deploymentDao.load(pending.getDeploymentId());
            EntityTableInfo entityTable = (EntityTableInfo) entityTableInfoService.getITableInfo(pending.getTableInfoId());
            final String tagetTable = entityTable.getTargetTableName();
            ProcessDefinitionImpl pd = (ProcessDefinitionImpl) workflowProcessService.getProcessDefinition(pending.getProcessId());
            ActivityImpl activity = null;
            if (null != pd) {
                activity = pd.getActivity(pending.getActivityName());
            }
            final List<DealInfo> dealinfoList = new ArrayList<>();
            for (Map.Entry<Long, List<Long>> entry : map.entrySet()) {
                Long consignorUserId = entry.getKey();
                List<Long> userList = entry.getValue();
                for (Long userId : userList) {
                    DealInfo di = new DealInfo();
                    di.setActivityName(pending.getActivityName());

                    di.setCreateTime(new Date());
                    di.setDealInfoType(DealInfoType.EXPECTEDCONSIGNOR);
                    di.setEntityCode(deployment.getEntityCode());
                    // di.setOutcome(execution.geto);
                    di.setProcessKey(deployment.getProcessKey());
                    // di.setOutcomeDes(outcomeDes)
                    di.setProcessVersion(deployment.getProcessVersion());
                    di.setTableInfoId(pending.getTableInfoId());
                    di.setTaskDescription(activity.getDescription());
                    di.setUserId(consignorUserId);
                    User user = userService.load(userId);
                    if (user != null) {
                        di.setAssignStaff(user.getStaff().getName());
                        di.setAssignStaffId("," + user.getId().toString() + ",");
                    }
                    di.setOutcomeDes("预期委托");
                    // di.setInstanceId(execution.get))
                    // long dealinfoId = idGenerator.getNextId(DealInfo.TABLE_NAME, userList.size() + 1);
                    // long entityDealinfoId = idGenerator.getNextId(DealInfo.TABLE_NAME, userList.size() + 1);
                    dealinfoList.add(di);

                }

            }
            pendingDao.getSessionFactory().getCurrentSession().doWork(new Work() {
                @Override
                public void execute(Connection conn) throws SQLException {
                    String dealSql = "INSERT INTO wf_deal_info (ID,CREATE_TIME,TABLE_INFO_ID,TASK_DESCRIPTION,USER_ID,VERSION,DEALINFO_TYPE,ASSIGN_STAFF,ENTITY_CODE,INSTANCE_ID,OUTCOME_DES,PROCESS_KEY,PROCESS_VERSION,ACTIVITY_NAME,TASK_DESCRIPTION_ZH_CN,OUTCOME_DES_ZH_CN) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    String dIDealSql = "INSERT INTO "
                            + DbUtils.getDealInfoTable(tagetTable)
                            + "(ID,CREATE_TIME,TABLE_INFO_ID,TASK_DESCRIPTION,USER_ID,VERSION,DEALINFO_TYPE,ASSIGN_STAFF,ENTITY_CODE,INSTANCE_ID,OUTCOME_DES,PROCESS_KEY,PROCESS_VERSION,ACTIVITY_NAME,TASK_DESCRIPTION_ZH_CN,OUTCOME_DES_ZH_CN) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    PreparedStatement ps = conn.prepareStatement(dealSql);
                    PreparedStatement ps1 = conn.prepareStatement(dIDealSql);
                    long dealinfoId = DbidGenerator.getNextId(DealInfo.TABLE_NAME, dealinfoList.size() + 1, null);
                    long entityDealinfoId = DbidGenerator.getNextId(DbUtils.getDealInfoTable(tagetTable), dealinfoList.size() + 1, null);
                    // ENTITY_CODE,INSTANCE_ID,OUTCOME_DES,PROCESS_KEY,PROCESS_VERSION
                    for (DealInfo dif : dealinfoList) {
                        ps.setLong(1, ++dealinfoId);
                        ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                        ps.setLong(3, dif.getTableInfoId());
                        ps.setString(4, dif.getTaskDescription());
                        ps.setLong(5, dif.getUserId());
                        ps.setInt(6, 0);
                        ps.setString(7, DealInfoType.EXPECTEDCONSIGNOR.toString());
                        ps.setString(8, dif.getAssignStaff());
                        ps.setString(9, dif.getEntityCode());
                        ps.setString(10, dif.getInstanceId());
                        ps.setString(11, dif.getOutcomeDes());
                        ps.setString(12, dif.getProcessKey());
                        ps.setInt(13, dif.getProcessVersion());
                        ps.setString(14, dif.getActivityName());
                        ps.setString(15, dif.getTaskDescription());
                        ps.setString(16, dif.getOutcomeDes());

                        ps1.setLong(1, ++entityDealinfoId);
                        ps1.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                        ps1.setLong(3, dif.getTableInfoId());
                        ps1.setString(4, dif.getTaskDescription());
                        ps1.setLong(5, dif.getUserId());
                        ps1.setInt(6, 0);
                        ps1.setString(7, DealInfoType.EXPECTEDCONSIGNOR.toString());
                        ps1.setString(8, dif.getAssignStaff());
                        ps1.setString(9, dif.getEntityCode());
                        ps1.setString(10, dif.getInstanceId());
                        ps1.setString(11, dif.getOutcomeDes());
                        ps1.setString(12, dif.getProcessKey());
                        ps1.setInt(13, dif.getProcessVersion());
                        ps1.setString(14, dif.getActivityName());
                        ps1.setString(15, dif.getTaskDescription());
                        ps1.setString(16, dif.getOutcomeDes());
                        ps.addBatch();
                        ps1.addBatch();
                    }

                    // long dealinfoId = idGenerator.getNextId(DealInfo.TABLE_NAME, userList.size() + 1);
                    // long entityDealinfoId = idGenerator.getNextId(DealInfo.TABLE_NAME, userList.size() + 1);

                    ps.executeBatch();
                    ps1.executeBatch();

                    ps.close();
                    ps1.close();
                }
            });

        }
    }

    private void generateDealInfo(OpenExecution execution, Deployment deployment, Map<Long, List<Long>> map) {
        if (!map.isEmpty()) {
            Activity active = execution.getActivity();
            final List<DealInfo> dealinfoList = new ArrayList<>();
            for (Map.Entry<Long, List<Long>> entry : map.entrySet()) {
                Long consignorUserId = entry.getKey();
                List<Long> userList = entry.getValue();
                for (Long userId : userList) {
                    DealInfo di = new DealInfo();
                    di.setActivityName(active.getName());
                    di.setCreateTime(new Date());
                    di.setDealInfoType(DealInfoType.EXPECTEDCONSIGNOR);
                    di.setEntityCode(deployment.getEntityCode());
                    di.setProcessKey(deployment.getProcessKey());
                    di.setProcessVersion(deployment.getProcessVersion());
                    di.setTableInfoId(execution.getTableInfoId());
                    di.setTaskDescription(((ActivityImpl) active).getDescription());
                    di.setUserId(consignorUserId);
                    User user = userService.load(userId);
                    if (user != null) {
                        di.setAssignStaff(user.getStaff().getName());
                        di.setAssignStaffId("," + user.getId().toString() + ",");
                    }
                    di.setOutcomeDes("预期委托");
                    dealinfoList.add(di);
                }
            }
            setExpectedConsignDealInfos(dealinfoList);
        }
    }

    private ThreadLocal<List<DealInfo>> expectedConsignDealInfos = new ThreadLocal<List<DealInfo>>(); //线程绑定预期委托处理意见

    public void setExpectedConsignDealInfos(List<DealInfo> dealInfos) {
        if (expectedConsignDealInfos != null) {
            expectedConsignDealInfos.set(dealInfos);
        }
    }

    private void getUsers(Set<Long> temp, List<Long> ids, Map<Long, List<Long>> expectedConsignor, List<Long> userList, List<Long> check) {
        for (Long id : ids) {
            if (expectedConsignor.get(id) != null && expectedConsignor.get(id).size() > 0) {
                if(check.contains(id)){
                    // 预期委托出现死循环的情况
                    StringBuffer sb = new StringBuffer();
                    for(Long d : check){
                        User user = (User) userService.load(d);
                        if(user!=null && user.getStaff()!=null){
                            sb.append(user.getStaff().getName()).append("->");
                        }
                    }
                    User user = (User) userService.load(id);
                    if(user!=null && user.getStaff()!=null){
                        sb.append(user.getStaff().getName());
                    }
                    throw new EcException(EcException.Code.EXPECTED_CONSIGN_CIRCULAR_REFERENCE, sb.toString());
                }else{
                    check.add(id);
                }
                for (Long target : expectedConsignor.get(id)) {
                    // 递归处理当前正在被处理的用户（id）的被委托用户列表
                    if (!temp.contains(target) || userList.contains(target)) {
                        temp.add(target);
                        getUsers(temp, expectedConsignor.get(id), expectedConsignor, userList, check);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void deletePendings(Long tableId, String activityName) {
        List<Object> param = new ArrayList<Object>(2);
        param.add(activityName);
        param.add(tableId);
        //发送删除MQ消息
        String deletePendingSql = "select id from Pending where activityName = ?0 and tableInfoId=?1";
        List<Object> iDList = pendingDao.findByHql(deletePendingSql, activityName, tableId);

        pendingDao.createQuery("delete Pending where activityName = ? and tableInfoId=?", param.toArray()).executeUpdate();

        if(null != iDList && iDList.size() !=0) {
//            deletePendingsToMQ(iDList);
        }

    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void deletePendings(String executionId) {
        List<Pending> pendings = pendingDao.findByCriteria(Restrictions.eq("executionId", executionId));

        //发送删除MQ消息
        String deletePendingSql = "select id from Pending where executionId=?0";
        List<Object> iDList = pendingDao.findByHql(deletePendingSql, new Object[] { executionId });

        for (Pending p : pendings) {
            pendingDao.delete(p);
        }

        if(null != iDList && iDList.size() !=0) {
//            deletePendingsToMQ(iDList);
        }

    }

    @Autowired
    private CountersignAssignStaffDaoImpl countersignAssignStaffDao;

    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    @Override
    public String getCountersignAssignStaff(Long deploymentId,String outcome,Long tableInfoId){
        CountersignAssignStaff cas=countersignAssignStaffDao.findEntityByCriteria(Restrictions.eq("deploymentId", deploymentId),Restrictions.eq("outcome", outcome),Restrictions.eq("tableInfoId", tableInfoId));
        if(cas!=null){
            return cas.getAssignStaff();
        }
        return "";
    }

    @Override
    @Transactional
    public void deleteCountersignAssignStaff(Long deploymentId,String outcome,Long tableInfoId){

        CountersignAssignStaff cas=countersignAssignStaffDao.findEntityByCriteria(Restrictions.eq("deploymentId", deploymentId),Restrictions.eq("outcome", outcome),Restrictions.eq("tableInfoId", tableInfoId));
        if(cas!=null){
            countersignAssignStaffDao.deletePhysical(cas);
        }
    }

    @Override
    @Transactional
    public void saveCountersignAssignStaff(Long deploymentId,String outcome,String assignStaff,Long tableInfoId){
        if(assignStaff==null||assignStaff.length()==0){
            return ;
        }
        CountersignAssignStaff cas=countersignAssignStaffDao.findEntityByCriteria(Restrictions.eq("deploymentId", deploymentId),Restrictions.eq("outcome", outcome),Restrictions.eq("tableInfoId", tableInfoId));
        if(cas==null){
            cas=new CountersignAssignStaff();
            cas.setTableInfoId(tableInfoId);
            cas.setOutcome(outcome);
            cas.setDeploymentId(deploymentId);
        }else{
            String assignStaffStr=cas.getAssignStaff();
            if(assignStaffStr!=null&&assignStaffStr.length()>0){
                assignStaff=assignStaffStr+","+assignStaff;
            }
        }
        cas.setAssignStaff(assignStaff);
        countersignAssignStaffDao.save(cas);
    }

}
