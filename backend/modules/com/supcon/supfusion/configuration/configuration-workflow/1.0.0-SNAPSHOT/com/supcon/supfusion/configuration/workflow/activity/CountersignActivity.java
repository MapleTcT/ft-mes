package com.supcon.supfusion.configuration.workflow.activity;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.Pending;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.workflow.entities.WorkFlowVar;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.activity.ExternalActivityBehaviour;
import org.jbpm.pvm.internal.env.Context;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.env.ExecutionContext;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CountersignActivity extends AbstractNoticeActivity implements ExternalActivityBehaviour {

	private static final long serialVersionUID = 1529591719898197198L;
	private transient static final Logger LOGGER = LoggerFactory.getLogger(CountersignActivity.class);

	protected TaskCommonProperty tcp;

	private Integer loop;

	@Override
	public void execute(ActivityExecution execution) throws Exception {
		execute((ExecutionImpl) execution);
		
	}
	
	private void findUserId(Long id, Long tempId, Map<Long, List<Long>> assigeUser, Map<Long, Set<Long>> relation) {
		if (assigeUser.get(tempId) != null) {
			List<Long> ids = assigeUser.get(tempId);
			for (Long i : ids) {
				findUserId(id, i, assigeUser, relation);
			}
		} else {
			if (relation.get(id) == null && id.longValue() != tempId.longValue()) {
				relation.put(id, new HashSet<Long>());
			}
			if (relation.get(id) != null) {
				relation.get(id).add(tempId);
			}
		}
	}

	protected void execute(ExecutionImpl execution) {
		ExecutionImpl concurrentRoot;// 主干
		if (Execution.STATE_ACTIVE_ROOT.equals(execution.getState())) {
			concurrentRoot = execution;// 直接在主干上
			execution.setState(Execution.STATE_INACTIVE_CONCURRENT_ROOT);
		} else if (Execution.STATE_ACTIVE_CONCURRENT.equals(execution.getState())) {
			concurrentRoot = execution.getParent();// 在枝干上,获取父亲
			execution.end();// 结束枝干
		} else{
			throw new AssertionError(execution.getState());
		}

		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		if(env !=null){
		WorkflowTaskService taskService = env.get(WorkflowTaskService.class);
		Deployment deployment = taskService.getDeployment(execution.getDeploymentId());
		// 获取并发人员
//		Set<User> users = new HashSet<User>();
		//Set<Long> userIds = new HashSet<Long>();
		Map<String, Set<Long>> userIds=new HashMap<String, Set<Long>>();
		Map<Long, String> entrust = new HashMap<Long, String>();
		try {
			tcp.getAssignmentHandler().assign(userIds, execution, entrust);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			if(e instanceof EcException && ((EcException) e).getCode().equals(EcException.Code.EXPECTED_CONSIGN_CIRCULAR_REFERENCE)){
				throw (EcException) e;
			}else{
				throw new EcException(e);
			}
		}
		
		Map<Long, List<Long>> assigeUser = tcp.getAssignmentHandler().getAssigeUser(execution, deployment);
		String taskName = null != tcp.getName() ? tcp.getName() : execution.getActivityName();
		ActivityImpl activity = execution.getActivity();
		activity.setDescription(tcp.getDescription());
		Set<Long> normalUser = new HashSet<Long>();
		Set<Long> expectedConsignor = new HashSet<Long>();
		Set<Long> allUser = new HashSet<Long>();
		if(userIds.get("normal")!=null&&userIds.get("normal").size()>0){
			normalUser.addAll(userIds.get("normal"));
		}
		if(userIds.get("expectedConsignor")!=null&&userIds.get("expectedConsignor").size()>0){
			expectedConsignor.addAll(userIds.get("expectedConsignor"));
		}
		Map<Long, Set<Long>> relation = new HashMap<Long, Set<Long>>();
		for(Long id : normalUser){
			findUserId(id, id, assigeUser, relation);
		}
		normalUser.addAll(expectedConsignor);
		for(Map.Entry<Long, String> entry : entrust.entrySet()){
			String[] ids = entry.getValue().split(",");
			for(String id : ids){
				if(id!=null && id.length()>0){
					Long lid = Long.parseLong(id);
					if(normalUser.contains(lid)){
						normalUser.remove(lid);
					}
				}
			}
		}
		allUser.addAll(normalUser);
		String mainCountersinger="";
		if(execution.getWorkFlowVar()!=null&&execution.getWorkFlowVar().getMainCountersigner()!=null){
			mainCountersinger=execution.getWorkFlowVar().getMainCountersigner();
		}
		
		if (!allUser.isEmpty()) {
		
/*			if (null == taskService){
				throw new WorkFlowException("can not find TaskService.");
			}*/
			// 生成工作流状态信息
			if (taskService.getRecallAbleFlag() && taskService.getLastTaskName() != null && !taskService.getLastTaskName().isEmpty()
					&& taskService.getLastTaskName().startsWith(execution.getDeploymentId() + "||")) {
				taskService.saveFlowCurrentStatus(execution);
			}

			List<ExecutionImpl> concurrentExecutions = new ArrayList<ExecutionImpl>();
			ExecutionContext originalContext = null;
			if (env != null) {
				//excution的上下文会发生变化，先去掉，在完成后set进来
				originalContext = (ExecutionContext) env.removeContext(Context.CONTEXTNAME_EXECUTION);
			}

			Pending pending;
			Iterator<Long> it = allUser.iterator();
			Long userId = null;
			List<Pending> pendings = new ArrayList<Pending>(userIds.size());
			
			// 实体和流程是否“支持移动端审批”
			boolean supportMobileApprove = taskService.checkDeploymentMobileApprove(deployment);

			// 会签选进来的人也要加进去
			try {
				while (it.hasNext()) {
					userId = it.next();
					ExecutionImpl concurrentExecution = concurrentRoot.createExecution(userId.toString());

					concurrentExecution.setTableInfoId(concurrentRoot.getTableInfoId());
//					concurrentExecution.setEntityId(concurrentRoot.getEntityId());
					concurrentExecution.setEntityCode(concurrentRoot.getEntityCode());
					concurrentExecution.setDeploymentId(concurrentRoot.getDeploymentId());
					concurrentExecution.setInitiatorPositionId(concurrentRoot.getInitiatorPositionId());
					concurrentExecution.setModelId(concurrentRoot.getModelId());
					concurrentExecution.setOwnerId(concurrentRoot.getOwnerId());
					concurrentExecution.setOwnerPositionId(concurrentRoot.getOwnerPositionId());
					concurrentExecution.setProcessInitiator(concurrentRoot.getProcessInitiator());
					concurrentExecution.setTableNo(concurrentRoot.getTableNo());
					concurrentExecution.setTableName(concurrentRoot.getTableName());
					concurrentExecution.setGroupEnabled(concurrentExecution.getGroupEnabled());
					concurrentExecution.setVariablesProvider(concurrentRoot.getVariablesProvider());
					concurrentExecution.setTransition(concurrentRoot.getTransition());
					concurrentExecution.setWorkFlowVar(concurrentRoot.getWorkFlowVar());
					concurrentExecution.setActivity(activity);
					concurrentExecution.setHistoryActivityStart(new Date());
					concurrentExecution.setState(Execution.STATE_ACTIVE_CONCURRENT);
					if (env != null) {
						env.setContext(new ExecutionContext(concurrentExecution));
					}
					
					concurrentExecutions.add(concurrentExecution);
					/***生成实例**************start**/
					String transitionName="";
					if(concurrentExecution.getTransition()!=null){
						transitionName=concurrentExecution.getTransition().getName();
					}else if(concurrentExecution.getWorkFlowVar()!=null){
						transitionName=concurrentExecution.getWorkFlowVar().getOutcome();
					}else{
						transitionName=concurrentExecution.getActivity().getOutgoingTransitions().get(0).getName();
					}
					concurrentExecution.historyActivityStart(transitionName);
					/***生成实例**************end**/
					pending = new Pending();
					pending.setOpenUrl(tcp.getOpenUrl());
					pending.setActivityName(taskName);
					pending.setActivityType(CountersignBinding.TAG);
					pending.setCreateTime(new Date());
					pending.setExecutionId(concurrentExecution.getId());
					pending.setTaskDescription(tcp.getDescription());
					pending.setStatus(Pending.ACTIVE);
					pending.setModelId(concurrentExecution.getModelId());
					pending.setDeploymentId(concurrentRoot.getDeploymentId());
					pending.setInstanceId(concurrentRoot.getProcessInstance().getId());
					pending.setTableInfoId(concurrentRoot.getTableInfoId());
//					pending.setEntityId(concurrentRoot.getEntityId());
					pending.setEntityCode(concurrentRoot.getEntityCode());
					pending.setTableNo(concurrentRoot.getTableNo());
//					if(entrust.get(userId)!=null){
//						pending.setProxySource(entrust.get(userId));
//					}
					StringBuffer sb = new StringBuffer();
					for (Map.Entry<Long, Set<Long>> entry : relation.entrySet()) {
						if (entry.getValue().contains(userId) && entry.getKey().longValue() != userId.longValue()) {
							sb.append(entry.getKey()).append(",");
						}
					}
					if (sb.length() > 0) {
						pending.setSourceStaff(sb.substring(0, sb.length() - 1));
					}
					if(expectedConsignor.contains(userId) && !userIds.get(Pending.NORMAL_PENDING).contains(userId)){ //对于user本身有权限，又被预期委托的，待办的sourceStaff要记下委托人id
						pending.setTaskType(2);
					}
					if(!mainCountersinger.equals("")){
						String[] mcUserIds=mainCountersinger.split(",");
						for(String mcUserId:mcUserIds){
							if(mcUserId.equals(userId.toString())){
								pending.setMainLoop(true);
							}
						}
					}
					pending.setUserId(userId);
					pending.setLoop(loop);
					pending.setStatus(Pending.ACTIVE);
					if (supportMobileApprove) {
						pending.setMobileApprove(tcp.getMobileApprove());
					} else {
						pending.setMobileApprove(false);
					}
					pendings.add(pending);

				}
				taskService.createPendings(pendings);
			} finally {
				if (env != null) {
					env.setContext(originalContext);
				}
			}
			// 消息提醒
			sendNotice(execution, allUser);// 发消息

		} else {
			// 没有任何人，那么抛出异常
			throw new EcException("ec.common.workflow.noPowerUsers",
					InternationalResource.get(tcp.getDescription()));
		}
	}
	}

	@Override
	public void signal(ActivityExecution execution, String signalName, Map<String, ?> parameters) throws Exception {
		signal((ExecutionImpl) execution, signalName, parameters);
	}
	protected void endEcecution(){
		
		
	}
	protected void signal(final ExecutionImpl execution, String signalName, Map<String, ?> parameters) {
		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		WorkflowTaskService taskService = env.get(WorkflowTaskService.class);
		ActivityImpl activity = execution.getActivity();

		// 删除工作流状态信息
		taskService.deleteFlowCurrentStatus(execution);

		if (Execution.STATE_ACTIVE_ROOT.equals(execution.getState())) {
			
			execution.take(signalName);
			taskService.deletePendings(execution.getTableInfoId(),execution.getActivityName());
			((ExecutionImpl) execution).historyActivityEnd();
		} else if (Execution.STATE_ACTIVE_CONCURRENT.equals(execution.getState())) {
			execution.fire(signalName, activity);
			// 删除Pending
			
			ExecutionImpl concurrentRoot = execution.getParent();
			
			//如果是强制结束，则删除其他人的实例和待办
			if(execution.getWorkFlowVar()!=null&&execution.getWorkFlowVar().getEndCountersignFlag()!=null&&execution.getWorkFlowVar().getEndCountersignFlag()){
				if(!concurrentRoot.getExecutions().isEmpty()){
					taskService.deletePendings(execution.getTableInfoId(),execution.getActivityName());
					ArrayList<ExecutionImpl> list= new ArrayList<ExecutionImpl>();
					list.addAll(concurrentRoot.getExecutions());
					Iterator<ExecutionImpl> iterator=list.iterator();
					while(iterator.hasNext()){
						ExecutionImpl it=iterator.next();
						if(!execution.getId().equals(it.getId())){
							((ExecutionImpl) it).historyActivityEnd();
							it.end();
							it.deleteHistory();
						}
					}
				}
			}else{
//				if (null != taskService) {
					taskService.deletePendings(execution.getId());
//				}
				// TODO 会签本身在流转时，撤回逻辑还未考虑明白，暂时不能撤回
				// 对于还未结束的会签，生成一条工作流状态信息
				// if (taskService.getRecallAbleFlag() && taskService.getLastTaskName() != null && !taskService.getLastTaskName().isEmpty()) {
				//	taskService.saveFlowCurrentStatus(execution);
				// }
			}
			
			((ExecutionImpl) execution).historyActivityEnd();
			
			execution.end();
			
			if (null != concurrentRoot) {
				WorkFlowVar wf=execution.getWorkFlowVar();
				Long deploymentId=wf.getDeploymentId();
				String outcome=wf.getOutcome();
				Long tableInfoId=wf.getTableInfoId();
				Set<Long> assignStaffset=wf.getAdditionalUsers();
				if(assignStaffset==null){
					assignStaffset=new HashSet<>();
				}
				if (concurrentRoot.getExecutions().isEmpty()) {//判断是否还有会签人，没有的话执行到下个环节
					if (parameters != null) {
						concurrentRoot.setVariables(parameters);
					}
					
//					if(wf!=null){
						String assignStaff=taskService.getCountersignAssignStaff(deploymentId, outcome, tableInfoId);
						if(assignStaff!=null&&assignStaff.length()>0){
							String[] arr=assignStaff.split(",");
							for(String str:arr){
								assignStaffset.add(Long.valueOf(str));
							}
							taskService.deleteCountersignAssignStaff(deploymentId, outcome, tableInfoId);
						}
						concurrentRoot.setWorkFlowVar(wf);
//					}
					concurrentRoot.fire(signalName, activity);
					concurrentRoot.setState(Execution.STATE_ACTIVE_ROOT);
					concurrentRoot.take(signalName);
				}else{//如果还 有其他人，则需要把本次这个人的指定人员给存下来
					
						StringBuilder sb=new StringBuilder();
						for(Long userId:assignStaffset){
							if(sb.length()>0){
								sb.append(",");
							}
							sb.append(userId.toString());
						}
						if(sb.length()>0){
							taskService.saveCountersignAssignStaff(deploymentId, outcome, sb.toString(), tableInfoId);
						}
					}
			}
		} else {
			throw new JbpmException("invalid execution state");
		}
	}

	public void setTcp(TaskCommonProperty tcp) {
		this.tcp = tcp;
	}
	public TaskCommonProperty getTcp() {
		return tcp;
	}
	public void setLoop(Integer loop) {
		this.loop = loop;
	}
	public Integer getLoop() {
		return loop;
	}
	
	private String getCurrentLanguage() {
		return "zh_CN";
	}

}
