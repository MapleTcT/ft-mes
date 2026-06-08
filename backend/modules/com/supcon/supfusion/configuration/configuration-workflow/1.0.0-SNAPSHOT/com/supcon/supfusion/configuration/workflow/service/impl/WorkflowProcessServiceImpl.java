package com.supcon.supfusion.configuration.workflow.service.impl;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.dao.PendingDaoImpl;
import com.supcon.supfusion.base.dao.TransitionStaffDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.dao.DeploymentDaoImpl;
import com.supcon.supfusion.base.enums.DataPermissionType;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.base.services.DataPermissionService;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.workflow.script.ScriptExecutor;
import com.supcon.supfusion.configuration.workflow.dao.FlowHistoryDaoImpl;
import com.supcon.supfusion.configuration.workflow.entities.ExpectedConsign;
import com.supcon.supfusion.configuration.workflow.entities.FlowHistory;
import com.supcon.supfusion.configuration.workflow.service.TransitionService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowProcessService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.jdbc.Work;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.RepositoryService;
import org.jbpm.api.model.Transition;
import org.jbpm.jpdl.internal.activity.TaskBinding;
import org.jbpm.pvm.internal.id.DbidGenerator;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.jbpm.pvm.internal.repository.RepositoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayInputStream;
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
public class WorkflowProcessServiceImpl implements WorkflowProcessService {
    
    @Autowired
    private DeploymentDaoImpl deploymentDao;
    @Autowired
    private PendingDaoImpl pendingDao;
    @Autowired
    private FlowHistoryDaoImpl flowHistoryDao;
    @Autowired
    private TransitionStaffDaoImpl transitionStaffDao;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private DataPermissionService dataPermissionService;
    @Autowired
    private EntityService entityService;
    @Autowired
    private ViewService viewService;
    @Autowired
    private WorkflowTaskService taskService;
    @Autowired
    private TransitionService transitionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    @Transactional
    public void update(Deployment deployment, String operatePower, String menuOperateStr, String actives, String updatePowerString, String superviseNamesMultiIDs, String selectStaffs, String linkRangeChage, String... env) {
        Assert.notNull(deployment);
        if(deployment.getProcessDefinitionId()==null||getProcessDefinition(deployment.getProcessDefinitionId())==null){
            throw new EcException(EcException.Code.PROCESS_IS_NOT_EXIST_AND_NEW_DEPLOY);
        }
        log.debug("workflow deploy");
        deploymentDao.save(deployment);
        log.debug("workflow deploy deploymentDao.save(deployment);");
        saveFlowHistory(deployment, "modify");
        log.debug("workflow deploy saveFlowHistory");
        ((RepositoryServiceImpl) repositoryService).updateDeploymentResource(deployment.getDeploymentId(), deployment.getName()
                + ".jpdl.xml", new ByteArrayInputStream(deployment.getProcessXml().getBytes()));
        if (menuOperateStr != null && !"".equals(menuOperateStr)) {
            publishMenuOperate(deployment.getProcessKey(), String.valueOf(deployment.getProcessVersion()), menuOperateStr,
                    deployment.getMenuInfoId(), deployment.getId(),deployment.getEntityCode(),deployment.getEntryUrl(), env);
        }
        // 生成权限
        dataPermissionService.updateMenuUserInfo(deployment.getProcessKey(), String.valueOf(deployment.getProcessVersion()), actives,
                operatePower, deployment.getEntityCode(), deployment.getMenuInfoId());
        log.debug("workflow deploy updateMenuUserInfo");
        dataPermissionService.saveWorkFlowPermissionChanges(deployment.getId(), updatePowerString);
        // 生成国际化
        // 格式：key,desc;key,desc;
//        internationalService.saveKeyDesc(deployment.getKeyDescs());
        //保存活动，迁移线信息
        dealPending4TaskProxyMod(deployment, null);
        saveTaskInfo(deployment.getProcessXml(),Long.valueOf(deployment.getId()));
        log.debug("workflow deploy saveTaskInfo");
        //刷新待办中url
        flushPendingUrl(deployment.getProcessXml(),Long.valueOf(deployment.getId()));
        // 预编译各种脚本
        preCompile(deployment);
        log.debug("workflow deploy preCompile");
        // 预处理单据入口迁移线
        List<Transition> ts = findFirstTransitions(deployment.getId());
        if (log.isDebugEnabled()) {
            log.debug("预缓存入口迁移线[{}]", ts);
        }
        //设置督办人
        saveSupervise(deployment.getId(),superviseNamesMultiIDs);
        //设置迁移线选人
        saveTranstionSelectStaffs(deployment.getId(),selectStaffs);
        // 流程实例缓存起来
//        flushProcessCache(deployment.getProcessDefinitionId());
        log.debug("workflow deploy flushProcessCache");
        //清楚路由的上次选人记录
        clearTransitionDefaultSelectStaff(deployment.getProcessKey(),linkRangeChage);
        //把该流程的所有版本的移动支持一起更新
        updateMobileApprove(deployment);

        //把该流程的所有版本的电子签名配置一起更新
        updateSignatureConfig(deployment);

        updateDeploymentMenu(deployment);
        log.debug("workflow deploy finish");
    }

    @Override
    @Transactional
    public void deploy(Deployment preDeployment, Deployment deployment, boolean setToBeCurrent, String operatePower, String menuOperatStr, String actives, String updatePowerString, String superviseNamesMultiIDs, String selectStaffs, String linkRangeChage, String... env) {
        Assert.notNull(deployment);
        if (null != preDeployment) {
            deploymentDao.save(preDeployment);
        }
        dealPending4TaskProxyMod(preDeployment, deployment);
        deploy(deployment);

        saveFlowHistory(deployment, "new");
        // 生成操作
        if (deployment.getMenuInfoId() != null) {

            publishMenuOperate(deployment.getProcessKey(), String.valueOf(deployment.getProcessVersion()), menuOperatStr,
                    deployment.getMenuInfoId(), deployment.getId(),deployment.getEntityCode(),deployment.getEntryUrl(), env);
        }
        // 生成权限
        dataPermissionService.updateMenuUserInfo(deployment.getProcessKey(), String.valueOf(deployment.getProcessVersion()), actives,
                operatePower, deployment.getEntityCode(), deployment.getMenuInfoId());
        //设置督办人
        saveSupervise(deployment.getId(),superviseNamesMultiIDs);
        //设置迁移线选人
        saveTranstionSelectStaffs(deployment.getId(),selectStaffs);
        dataPermissionService.saveWorkFlowPermissionChanges(deployment.getId(), updatePowerString);
        //把该流程的所有版本的移动支持一起更新
        updateMobileApprove(deployment);

        //把该流程的所有版本的电子签名配置一起更新
        updateSignatureConfig(deployment);

        //清楚路由的上次选人记录
        clearTransitionDefaultSelectStaff(deployment.getProcessKey(),linkRangeChage);
        updateDeploymentMenu(deployment);
    }

    @Transactional(readOnly = true,propagation= Propagation.SUPPORTS)
    public List<Transition> findFirstTransitions(long deploymentId) {
        Deployment d = getDeployment(deploymentId);
        if (null != d) {
            ProcessDefinitionImpl pd = (ProcessDefinitionImpl) getProcessDefinition(d.getProcessDefinitionId());
            // ProcessDefinitionImpl pd = processDefinitionDao.load(d.getProcessDefinitionId());
            if (null != pd) {
                ActivityImpl initialActivity = pd.getInitial();
                if (null != initialActivity) {
                    ActivityImpl activity = initialActivity;
                    while (!TaskBinding.TAG.equals(activity.getType())) {
                        List<TransitionImpl> transitions = (List<TransitionImpl>) activity.getOutgoingTransitions();
                        if (transitions.isEmpty() || transitions.size() > 1) {
                            throw new EcException("Not suitable start activity.");
                        }
                        TransitionImpl transition = transitions.get(0);
                        activity = transition.getDestination();
                    }
                    List<Transition> transitions = (List<Transition>) activity.getOutgoingTransitions();
                    Collections.sort(transitions, new Comparator<Transition>() {

                        @Override
                        public int compare(Transition o1, Transition o2) {
                            if(o1.getRouterSequence()!=0&&o2.getRouterSequence()!=0){//都不等于0，就比较大小
                                if(o1.getRouterSequence()>o2.getRouterSequence()){
                                    return 1;
                                }else if(o1.getRouterSequence()<o2.getRouterSequence()){
                                    return -1;
                                }else{
                                    return 0;
                                }
                            }
                            //等于0的排后面
                            if(o1.getRouterSequence()!=0&&o2.getRouterSequence()==0){
                                return -1;
                            }else if(o1.getRouterSequence()==0&&o2.getRouterSequence()!=0){
                                return 1;
                            }
                            if(o1.getReject()==1&&o2.getReject()==0){
                                return 1;
                            }
                            if(o1.getReject()==0&&o2.getReject()==1){
                                return -1;
                            }
                            return 0;

                        }
                    });
                    return transitions;
                }
            }
        }
        return Collections.emptyList();

    }

    public void saveFlowHistory(Deployment dm, String acitonMode) {
        FlowHistory flowHistory = new FlowHistory();
        flowHistory.setDeploymentId(dm.getId());
        flowHistory.setFlowXML(dm.getProcessXml());
        flowHistory.setProcessKey(dm.getProcessKey());
        flowHistory.setProcessVersion(dm.getProcessVersion());
        flowHistory.setPublishTime(new Date());
        // flowHistory.setUserName(get);
        if (dm.getModifyStaffId() != null) {
            flowHistory.setStaffId(dm.getModifyStaffId());
        } else {
            flowHistory.setStaffId(dm.getCreateStaffId());
        }
        flowHistory.setPublishType(acitonMode);
        flowHistoryDao.save(flowHistory);
    }

    @Override
    public ProcessDefinition getProcessDefinition(String processDefinitionId) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).uniqueResult();
    }

    @Transactional(readOnly = true,propagation=Propagation.SUPPORTS)
    public Deployment getDeployment(long id) {
        return deploymentDao.get(id);
    }


    public void publishMenuOperate(String flowKey, String flowVersion, String menuOperatStr, Long menuId, Long deploymentId,String entityCode,String entryUrl, String... env) {
        if (null == menuOperatStr || "".equals(menuOperatStr)) {
            return;
        }
        //查询viewCode
        List<View> views = viewService.findViewByUrl(entryUrl);
        String viewCode = "";
        if (!ObjectUtils.isEmpty(views)){
            viewCode = views.get(0).getCode();
        }
        String[] activeArr = menuOperatStr.split("\\$");
        MenuInfo menuInfo = menuInfoService.load(menuId);
        for (int i = 0; i < activeArr.length; i++) {
            String active = activeArr[i];
            String[] operateArr = active.split("\\|");
            String code = operateArr[0];
            String name = operateArr[1];
            String url = operateArr[2];
            String type = operateArr[3];
            Boolean ignorePermission = Boolean.parseBoolean(operateArr[4]);
            //不需要分配权限就不生成操作
            if (ignorePermission){
                continue;
            }
            Integer index = url.lastIndexOf("/");
            Boolean isAllowProxy = true;
            Integer msgFlag = 0;
            if (operateArr.length > 5) {
//				msgFlag = Integer.valueOf(operateArr[5]);
                for(int j=5;j<operateArr.length;j++){
                    if(operateArr[j].indexOf("isAllowProxy:")>-1){
                        isAllowProxy = Boolean.parseBoolean(operateArr[j].replace("isAllowProxy:", ""));
                    }else if(operateArr[j].indexOf("remindFlag:")>-1){
                        msgFlag = Integer.valueOf(operateArr[j].replace("remindFlag:", ""));
                    }
                }
            }
            String nameSpace = "";
            if (url.startsWith("'")) {
                nameSpace = url.substring(1, index);
            } else {
                nameSpace = url.substring(0, index);
            }
            String action = url.substring(index + 1, url.length());
            MenuOperate menuOperate = menuInfoService.getMenuOperateByFlow(flowKey, flowVersion, code);
            if (menuOperate == null) {
                menuOperate = new MenuOperate();
            }
//            menuOperate.setIsAllowProxy(isAllowProxy);
            menuOperate.setMsgAssembled(msgFlag);
            menuOperate.setAction(action);
            menuOperate.setCode(code);
            menuOperate.setDeploymentId(deploymentId);
            menuOperate.setMenuInfo(menuInfo);
            menuOperate.setCompany(menuInfo.getCompany());
            menuOperate.setFlowKey(flowKey);
            menuOperate.setFlowVersion(flowVersion);
             menuOperate.setMenuOperateType(MenuOperateType.FLOWOPERATE);
            menuOperate.setMenuOperateType(MenuOperateType.valueOf(type));
            menuOperate.setName(name);
            menuOperate.setNamespace(nameSpace);
            menuOperate.setUrl(url);
            menuOperate.setCompany(menuInfo.getCompany());
            menuOperate.setCid(menuInfo.getCid());
            menuOperate.setValid(true);
            menuOperate.setVersion(0);
            menuOperate.setModule(menuInfo.getModuleCode());
            menuOperate.setEntityCode(entityCode);
            menuOperate.setViewCode(viewCode);
            if(code.toLowerCase().startsWith("start")){
                menuOperate.setEnableAssignPos(false);
                menuOperate.setEnableAssignStaff(false);
                menuOperate.setEnableGroupRestrict(false);
                menuOperate.setEnableNoRestrict(true);
                menuOperate.setEnablePosRestrict(false);
            }else{
                menuOperate.setEnableAssignPos(true);
                menuOperate.setEnableAssignStaff(true);
                if(entityService.getEntity(entityCode).getGroupEnabled()){
                    menuOperate.setEnableGroupRestrict(true);
                }else{
                    menuOperate.setEnableGroupRestrict(false);
                }
                menuOperate.setEnableNoRestrict(true);
                menuOperate.setEnablePosRestrict(true);
            }

            menuOperate.setIgnorePermission(ignorePermission);
            menuInfoService.saveMenuOperate(menuOperate);
        }

    }

    private void dealPending4TaskProxyMod(Deployment preDeployment,
                                          Deployment deployment) {
        // TODO Auto-generated method stub
        if(null==preDeployment)
            return;
        Document document;
        try {
            if(deployment == null){
                //更新发布
                document = DocumentHelper.parseText(preDeployment.getProcessXml());
            }else{
                //全量发布
                document = DocumentHelper.parseText(deployment.getProcessXml());
            }
            String[] nodes = { "start", "task","notification", "auto", "decision","countersign", "fork", "join", "sub-process", "end","end-cancel" };
            for (int i = 0; i < nodes.length; i++) {
                Element root = document.getRootElement();
                List list = root.elements(nodes[i]);
                Iterator<Element> iter = list.iterator();
                while (iter.hasNext()) {
                    Element element = iter.next();
                    String taskCode = element.attributeValue("name");
                    Task preTask =taskService.getTask(taskCode, preDeployment.getId());
                    if(preTask != null && element.attributeValue("isAllowProxy") != null){
                        Boolean preIsAllowProxy = (null == preTask.getIsAllowProxy()) ? false : preTask.getIsAllowProxy();
                        Boolean curIsAllowProxy = Boolean.parseBoolean(element.attributeValue("isAllowProxy"));
                        if(!preIsAllowProxy&&curIsAllowProxy){
                            //如果原先该环节已经被设置为“不允许委托”，如果修改为“允许委托”，则只修改流程实例，``不恢复``之前被删除的委托待办和预期委托数据，需要用户重新进入委托管理或待办管理中进行操作
                        }
                        else if(preIsAllowProxy&&!curIsAllowProxy){
                            //1.获取预期委托（当前活动且不为ALL）的记录，进行撤销操作
                            List<ExpectedConsign> expectedConsignList =
                                    taskService.getExpectedConsignList(preTask.getProcessKey(),preTask.getCode(),1);
                            taskService.expectedConsignRecallSchedule(expectedConsignList,3);

                        }
                    }
                }
            }
        }catch (DocumentException e) {
            log.error(e.getMessage());
            throw new EcException(EcException.Code.DOCUMENT_ERROR);
        } finally {
            document = null;
        }

    }

    @Transactional
    public void saveTaskInfo(String xmlStr, Long deploymentId){
        Document document;
        try {
            document = DocumentHelper.parseText(xmlStr);
            String[] nodes = { "start", "task","notification", "auto", "decision","countersign", "fork", "join", "sub-process", "end","end-cancel" };
            for (int i = 0; i < nodes.length; i++) {
                Element root = document.getRootElement();
                List list = root.elements(nodes[i]);
                Iterator<Element> iter = list.iterator();
                while (iter.hasNext()) {
                    Element element = iter.next();
                    String taskCode=saveTask(nodes[i],element,deploymentId);
                    List transitionList = element.elements("transition");
                    if (transitionList != null) {
                        Iterator<Element> transitionIter = transitionList.iterator();
                        while (transitionIter.hasNext()) {
                            Element transitionElement = transitionIter.next();
                            saveTransition(transitionElement,deploymentId,taskCode);
//							transformInternational(transitionElement, language);
                        }
                    }

//					transformInternational(element, language);
                }
            }
//			xmlStr = document.asXML().toString();
        } catch (DocumentException e) {
            log.error(e.getMessage());
            throw new EcException(EcException.Code.DOCUMENT_ERROR);
        } finally {
            document = null;
        }
    }

    @Transactional
    public void saveTransition(Element e,Long deploymentId,String taskCode){
        com.supcon.supfusion.base.entities.Transition transition = new com.supcon.supfusion.base.entities.Transition();
        String Uid = e.attributeValue("name");
        transition.setCode(Uid);
        transition.setVersion(0);
        transition.setName(e.attributeValue("internationalKey"));
        transition.setFromNodeCode(taskCode);
        transition.setToNodeCode(e.attributeValue("to"));
        transition.setDeploymentId(deploymentId);
        if(e.attributeValue("sequence")!=null){//迁移线的路由顺序为空的可以把0存进去，在后期排序已经处理过，不会排在最前
//			if(!e.attributeValue("sequence").equals("")&&Integer.parseInt(e.attributeValue("sequence"))>0){
            transition.setRouteSequence(Integer.parseInt(e.attributeValue("sequence")));
//			}
        }
        if(e.attributeValue("reject")!=null&&e.attributeValue("reject").toString().equals("1")){//reject==1表示驳回
            transition.setType(2);
        }else if(e.attributeValue("cancel")!=null&&e.attributeValue("cancel").toString().equals("1")){
            transition.setType(3);
        }else if(e.attributeValue("notificationType")!=null&&e.attributeValue("notificationType").toString().equals("1")){
            transition.setType(4);
        }else{
            transition.setType(1);
        }

        if(e.attributeValue("selectStaff")==null){
            transition.setSelectStaff("0");
        }else{
            transition.setSelectStaff(e.attributeValue("selectStaff"));
        }

        if(e.attributeValue("requiredStaff")!=null){
            transition.setRequiredStaff(true);
        }else{
            transition.setRequiredStaff(false);
        }
        if(e.attributeValue("defaultSelectStaff")!=null){
            transition.setDefaultSelectStaff(true);
        }else{
            transition.setDefaultSelectStaff(false);
        }

        List<Element> conditionList = e.elements("condition");
        if(conditionList.size()>0){
            transition.setExpression(conditionList.get(0).attributeValue("expr"));
        }
        transitionService.save(transition);
    }

    @Transactional
    public String saveTask (String nodeType,Element e,Long deploymentId){
        String taskCode = e.attributeValue("name");
        Task task =taskService.getTask(taskCode, deploymentId);
        if(task==null){
            task = new Task();
            task.setVersion(0);
            Deployment dm = deploymentDao.load(deploymentId);
            if(null != dm) {
                task.setProcessKey(dm.getProcessKey());
                task.setProcessVersion(dm.getProcessVersion());
            }
        }
        task.setCode(taskCode);
        task.setName(e.attributeValue("internationalKey"));
        task.setDeploymentId(deploymentId);
        if(e.attributeValue("isAllowProxy")!=null){
            task.setIsAllowProxy(Boolean.parseBoolean(e.attributeValue("isAllowProxy")));
        }else{
            task.setIsAllowProxy(true);
        }
        if(nodeType.equals("start")){
            task.setType(1);
            task.setViewCode(e.attributeValue("viewCode"));
        }else if(nodeType.equals("end")){
            task.setType(2);
            setReminderType(e,task);
        }else if(nodeType.equals("end-cancel")){
            task.setType(3);
        }else if(nodeType.equals("task")||nodeType.equals("notification")){
            if(nodeType.equals("task")){
                task.setType(4);
            }else{
                task.setType(5);
            }
            List<Element> viewList = e.elements("open-action");
            if (viewList.size()>0) {
                task.setViewCode(viewList.get(0).attributeValue("viewCode"));
                task.setOpenMode(viewList.get(0).attributeValue("target"));
            }
            setReminderType(e,task);
            if(e.attributeValue("bulkDealFlag")!=null){
                task.setBatchProcess(true);
            }
            /*
             * if(e.attributeValue("dealinfoFlag")!=null){ task.setForbiddenComment(true); }
             */

            if(e.attributeValue("ignorePermission")!=null) {
                task.setIgnorePermission(Boolean.parseBoolean(e.attributeValue("ignorePermission").toString()));
            }

            if(e.attributeValue("dealSet")!=null) {
                task.setDealSet(Integer.parseInt(e.attributeValue("dealSet").toString()));
            }
            if(e.attributeValue("customParam")!=null){
                task.setCustomParam(e.attributeValue("customParam"));
            }
            if(e.attributeValue("recallAble")!=null){
                task.setRecallAble(Boolean.parseBoolean(e.attributeValue("recallAble")));
            }
            if(e.attributeValue("webSignetFalg")!=null){
                task.setWebSignetFlag(Boolean.parseBoolean(e.attributeValue("webSignetFalg")));
            }

            if(e.attributeValue("showInSimpleDealInfo")!=null){
                task.setShowInSimpleDealInfo(Boolean.parseBoolean(e.attributeValue("showInSimpleDealInfo")));
            }
            if(e.attributeValue("mobileApprove")!=null){
                task.setMobileApprove(Boolean.parseBoolean(e.attributeValue("mobileApprove")));
            }

            setCandidate(e,task);
            if(e.attributeValue("sequence")!=null){
                if(!e.attributeValue("sequence").equals("")&&Integer.parseInt(e.attributeValue("sequence"))>0){
                    task.setRouteSequence(Integer.parseInt(e.attributeValue("sequence")));
                }
            }
            if(e.attributeValue("requiredTime")!=null&&e.attributeValue("requiredTime").length()>0){
                task.setRequiredTime(new java.math.BigDecimal(e.attributeValue("requiredTime")));
            }
            if(e.attributeValue("overdueReminders")!=null){
                task.setOverdueReminders(true);
            }
        }else if(nodeType.equals("auto")){
            task.setType(6);
            List<Element> scriptList = e.elements("auto-script");
            if (scriptList != null) {
                task.setScript(scriptList.get(0).attributeValue("code"));
            }
            setReminderType(e,task);
        }else if(nodeType.equals("countersign")){
            task.setType(7);
            List<Element> viewList = e.elements("open-action");
            if (viewList != null) {
                task.setViewCode(viewList.get(0).attributeValue("viewCode"));
                task.setOpenMode(viewList.get(0).attributeValue("target"));
            }
            setReminderType(e,task);
            if(e.attributeValue("bulkDealFlag")!=null){
                task.setBatchProcess(true);
            }

            /*
             * if(e.attributeValue("dealinfoFlag")!=null){ task.setForbiddenComment(true); }
             */
            if(e.attributeValue("webSignetFalg")!=null){
                task.setWebSignetFlag(Boolean.parseBoolean(e.attributeValue("webSignetFalg")));
            }
            if(e.attributeValue("ignorePermission")!=null) {
                task.setIgnorePermission(Boolean.parseBoolean(e.attributeValue("ignorePermission").toString()));
            }

            if(e.attributeValue("dealSet")!=null) {
                task.setDealSet(Integer.parseInt(e.attributeValue("dealSet").toString()));
            }
            if(e.attributeValue("loop")!=null){
                task.setLoopCountersign(Integer.parseInt(e.attributeValue("loop")));
            }
            if(e.attributeValue("recallAble")!=null){
                task.setRecallAble(Boolean.parseBoolean(e.attributeValue("recallAble")));
            }else{
                task.setRecallAble(false);
            }
            if(e.attributeValue("showInSimpleDealInfo")!=null){
                task.setShowInSimpleDealInfo(Boolean.parseBoolean(e.attributeValue("showInSimpleDealInfo")));
            }
            if(e.attributeValue("mobileApprove")!=null){
                task.setMobileApprove(Boolean.parseBoolean(e.attributeValue("mobileApprove")));
            }
//			if(e.attributeValue("crossCompany")!=null){
//				task.setCrossCompany(true);
//			}
            if(e.attributeValue("customParam")!=null){
                task.setCustomParam(e.attributeValue("customParam"));
            }
            setCandidate(e,task);
            if(e.attributeValue("sequence")!=null){
                if(!e.attributeValue("sequence").equals("")&&Integer.parseInt(e.attributeValue("sequence"))>0){
                    task.setRouteSequence(Integer.parseInt(e.attributeValue("sequence")));
                }
            }
            if(e.attributeValue("requiredTime")!=null&&e.attributeValue("requiredTime").length()>0){
                task.setRequiredTime(new java.math.BigDecimal(e.attributeValue("requiredTime")));
            }
            if(e.attributeValue("overdueReminders")!=null){
                task.setOverdueReminders(true);
            }
        }else if(nodeType.equals("decision")){
            task.setType(8);
            List<Element> handlerList = e.elements("handler");
            if(handlerList.size()>0){
                task.setExternalComponent(handlerList.get(0).attributeValue("class"));
            }
        }else if(nodeType.equals("fork")){
            task.setType(9);
            e.attributeValue("multiplicity");

        }else if(nodeType.equals("join")){
            task.setType(10);
            if(e.attributeValue("multiplicity")!=null){
                task.setJoinCount(Integer.parseInt(e.attributeValue("multiplicity")));
            }
        }else if(nodeType.equals("sub-process")){
            task.setType(11);
            task.setSubProcessKey(e.attributeValue("sub-process-key"));
        }
        taskService.save(task);
        return taskCode;
    }

    public void flushPendingUrl(String xmlStr,final Long deploymentId){
        Document document;
        final Map<String, String> map=new HashMap<>();;
        try {
            document = DocumentHelper.parseText(xmlStr);
            String[] nodes = {  "task","notification","countersign" };
            for (int i = 0; i < nodes.length; i++) {
                Element root = document.getRootElement();
                List list = root.elements(nodes[i]);
                Iterator<Element> iter = list.iterator();
                while (iter.hasNext()) {
                    Element element = iter.next();
                    //String taskCode=saveTask(nodes[i],element,deploymentId);
                    List<Element> viewList = element.elements("open-action");
                    String taskCode = element.attributeValue("name");
                    String viewCode="";
                    if (viewList.size()>0) {
                        viewCode=viewList.get(0).attributeValue("viewCode");
                    }
                    View v = viewService.getView(viewCode);
                    if(v==null){
                        continue;
                    }
                    String url=v.getUrl();
                    String customParam=element.attributeValue("customParam");
                    if(customParam!=null && customParam.length()>0){
                        url+="?"+customParam;
                    }
                    map.put(taskCode,url);


                }
            }
            pendingDao.getSession().doWork(new Work() {
                @Override
                public void execute(Connection conn) throws SQLException {
                    String sql = "update wf_pending set OPEN_URL=? where  DEPLOYMENT_ID=? and ACTIVITY_NAME=?";
                    PreparedStatement ps = conn.prepareStatement(sql);

                    for (Map.Entry<String, String> entity : map.entrySet()) {
                        ps.setString(1,entity.getValue());
                        ps.setLong(2, deploymentId);
                        ps.setString(3, entity.getKey());
                        ps.addBatch();
                        if (log.isDebugEnabled())
                            log.debug(sql);
                    }
                    ps.executeBatch();
                    ps.close();
                }
            });

        } catch (DocumentException e) {
            log.error(e.getMessage());
            throw new EcException(EcException.Code.DOCUMENT_ERROR);
        } finally {
            document = null;
        }
    }

    private void preCompile(Deployment deployment) {
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(deployment.getProcessXml());
        } catch (DocumentException e) {
        }
        try{
            if (null != doc) {
                Element root = doc.getRootElement();
                // 处理自动活动
                List<Element> autos = root.elements("auto");
                if (null != autos && !autos.isEmpty()) {
                    for (Element auto : autos) {
                        Element scriptE = auto.element("auto-script");
                        if (null != scriptE) {
                            String script = scriptE.getTextTrim();
                            if (script.length() > 0) {
                                ScriptExecutor.preCompile(script);//
                            }
                        }
                    }
                }
                // 处理路由条件
                List<Element> decisions = root.elements("decision");
                if (null != decisions && !decisions.isEmpty()) {
                    for (Element decisionE : decisions) {
                        if (null != decisionE) {
                            List<Element> tsE = decisionE.elements("transition");
                            if (null != tsE && !tsE.isEmpty()) {
                                for (Element tE : tsE) {
                                    if (null != tE) {
                                        Element cE = tE.element("condition");
                                        if (null != cE && cE.attributeValue("expr") != null) {
                                            String script = cE.attributeValue("expr").trim();
                                            if (script.length() > 0)
                                                ScriptExecutor.preCompile(script);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            log.error(e.getMessage(), e);
//            throw new BAPException(BAPException.Code.PRECOMPILEERROR);
        }
    }

    public void saveSupervise(Long deploymentId,String superviseNamesMultiIDs){

        String deleteSql="delete from wf_supervise where deployment_id=?";
        jdbcTemplate.update(deleteSql, deploymentId);
        if(superviseNamesMultiIDs==null||superviseNamesMultiIDs.equals("")){
            return ;
        }
        List<String> sqlArray=new ArrayList<String>();
        String[] ids=superviseNamesMultiIDs.split(",");
        for(String idStr:ids){
            Long maxID= DbidGenerator.getNextId("wf_supervise", 1, null);
            String sql="insert into wf_supervise (version,DEPLOYMENT_ID,STAFF,valid,id) values (0,"+deploymentId+","+idStr+",1,"+maxID+")";
            sqlArray.add(sql);
        }
        if(sqlArray.size()>0){
            String sqls[]=new String[sqlArray.size()];
            sqlArray.toArray(sqls);
            jdbcTemplate.batchUpdate(sqls);
        }
    }

    @Transactional
    public void saveTranstionSelectStaffs(Long deploymentId,String selectStaffs){
        transitionStaffDao.bulkExecute("delete from TransitionStaff where deploymentId=?0", deploymentId);
        if(selectStaffs==null||selectStaffs.length()==0){
            return ;
        }
        String[] selectStaffArr=selectStaffs.split(";");
        for(String selectStr:selectStaffArr){
            TransitionStaff ts=new TransitionStaff();
            String[] arr=selectStr.split(",");
            String outcome=arr[0];
            DataPermissionType type=DataPermissionType.valueOf(arr[1].trim());
            Long typeId=Long.valueOf(arr[2]);
            ts.setDeploymentId(deploymentId);
            ts.setOutcome(outcome);
            if(arr.length>3&&arr[3]!=null&&arr[3].length()>0){
                ts.setGroupName(arr[3]);
            }
            if(arr.length>4&&arr[4]!=null&&arr[4].length()>0){
                Double order=null;
                try {
                    order=Double.valueOf(arr[4]);
                } catch (Exception e) {
                }
                ts.setSort(order);
            }
            ts.setType(type);
            ts.setTypeId(typeId);
            ts.setValid(true);
            ts.setVersion(0);
            transitionStaffDao.save(ts);
        }
    }

    public void clearTransitionDefaultSelectStaff(String flowKey,String links){
        if(flowKey==null || flowKey.length()==0 || links==null || links.length()==0){
            return ;
        }
        String deleteSql="delete from base_cookie where TYPE like ? ";
        String link[] = links.split(";");
        for(int i=0;i<link.length;i++){
            String type="DEFAULT_SELECTSTAFF_"+flowKey+"_"+link[i];
            pendingDao.createNativeQuery(deleteSql, type).executeUpdate();
        }

    }

    @Transactional
    public void updateMobileApprove(Deployment deployment){
        deploymentDao.bulkExecute("update Deployment set mobileapprove = ? where processKey = ? and valid = true", deployment.getMobileapprove(),
                deployment.getProcessKey());
    }

    @Transactional
    public void updateSignatureConfig(Deployment deployment){
        deploymentDao.bulkExecute("update Deployment set signatureEnable = ? where processKey = ? and valid = true", deployment.getSignatureEnable(),
                deployment.getProcessKey());
    }

    @Transactional
    public void updateDeploymentMenu(Deployment deployment){
        deploymentDao.bulkExecute("update Deployment set menuInfoId = ? where valid=true and processVersion>0 and processKey = ? and id != ?", deployment.getMenuInfoId(),deployment.getProcessKey(),deployment.getId());
    }

    public void setReminderType(Element e,Task task){
        String reminderType ="";
        List<Element> noticeList = e.elements("notice");
        if (noticeList.size()>0) {
            List<Element> emailList=noticeList.get(0).elements("email");
            if(emailList.size()>0){
                reminderType="email,";
            }
            List<Element> jabberList=noticeList.get(0).elements("jabber");
            if(jabberList.size()>0){
                reminderType=reminderType+"jabber,";
            }
            List<Element> smsList=noticeList.get(0).elements("sms");
            if(smsList.size()>0){
                reminderType=reminderType+"sms";
            }
            if(reminderType.endsWith(",")){
                reminderType = reminderType.substring(0, reminderType.length()-1);
            }
            task.setReminderType(reminderType);
        }
    }

    public void setCandidate(Element e,Task task){
        List<Element> handlerList = e.elements("assignment-handler");
        if(handlerList.size()>0){
            List<Element> fieldList=handlerList.get(0).elements("field");
            if(fieldList.size()>0){
                //候选人在assignment-handler里是第6个field，<field name="staffIds"><string value="aa"/></field>
                for(int i=0;i<fieldList.size();i++){
                    if(fieldList.get(i).attributeValue("name")!=null&&fieldList.get(i).attributeValue("name").equals("staffIds")){
                        List<Element> sList=fieldList.get(i).elements("string");
                        task.setCandidate(sList.get(0).attributeValue("value"));
                    }
                }
            }
        }
    }

    @Transactional
    public void deploy(Deployment deployment) {
        Assert.notNull(deployment, "the deployment can not be null.");
        String resourceName = deployment.getName() + ".jpdl.xml";
        String deploymentId = repositoryService.createDeployment().addResourceFromString(resourceName, deployment.getProcessXml()).deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).uniqueResult();
        if (null == processDefinition)
            throw new JbpmException("can not found the deployment.");
        deployment.setDeploymentId(deploymentId);
        //deployment.setDescription(processDefinition.getDescription());
        deployment.setIsSuspended(false);
        deployment.setProcessDefinitionId(processDefinition.getId());
        deployment.setProcessKey(processDefinition.getKey());
        deployment.setProcessVersion(processDefinition.getVersion());
        deployment.setProcessName(processDefinition.getName());
        // 重置是否当前版本
        if (null != deployment.getIsCurrentVersion() && deployment.getIsCurrentVersion()) {
            deploymentDao.bulkExecute("update Deployment set isCurrentVersion = false,operatePowers='' where processKey = ?0",
                    deployment.getProcessKey());
        }
        // 如果新发布的流程在自己的deployment中已经存在版本一样流程，需要删除流程！
        Object[] params = new Object[2];
        params[0] = deployment.getProcessKey();
        params[1] = deployment.getProcessVersion();
        deploymentDao.createQuery("delete from Deployment where processKey=? and processVersion=?", params);
        // 生成国际化
//        internationalService.saveKeyDesc(deployment.getKeyDescs());
        // save
        if (deployment.getVersion() == null) {
            deployment.setVersion(0);
        }
        deploymentDao.save(deployment);
        //保存活动、迁移线信息
        saveTaskInfo(deployment.getProcessXml(), Long.valueOf(deployment.getId()));
        // 流程实例缓存起来
//        flushProcessCache(deployment.getProcessDefinitionId());

        if (log.isDebugEnabled()) {
            log.debug("\n=============================================================================\n" + deployment.getProcessXml()
                    + "\n=============================================================================\n");
        }
        // 预编译各种脚本
        preCompile(deployment);
        // 预处理单据入口迁移线
        List<Transition> ts = findFirstTransitions(deployment.getId());
        if (log.isDebugEnabled()) {
            log.debug("预缓存入口迁移线[{}]", ts);
        }

    }


}
