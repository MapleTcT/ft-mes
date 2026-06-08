package com.supcon.supfusion.configuration.workflow.handlers;

import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetUserListHandler extends AbstractAssignmentHandler {

	private static final long serialVersionUID = 4988942208840404098L;
	private transient static final Logger LOGGER = LoggerFactory.getLogger(GetUserListHandler.class);

	private String inputorFlag;
	private String leaderFlag;
	private String bigLeaderFlag;
	private String flowDealFlag;
	private String activityDealFlag;
	private String attentFlag;
	private WorkflowTaskService taskService;
	private String staffIds;
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void assign(Map<String, Set<Long>> userIds, OpenExecution execution,
			Map<Long, String> entrust) throws Exception {
		Long start=System.currentTimeMillis();
		EnvironmentImpl env = getEnv();
		taskService = env.get(WorkflowTaskService.class);
		taskService.getUserList(userIds, execution, inputorFlag, leaderFlag, bigLeaderFlag, staffIds,flowDealFlag,activityDealFlag,attentFlag,entrust);
		LOGGER.debug("================ GetUserListHandler Cost {}ms. ==================", System.currentTimeMillis() - start);
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Map<Long, List<Long>> getAssigeUser(OpenExecution execution, Deployment deployment){
		return taskService.getAssigeUser(execution, deployment);
	}

	/**
	 * 这里的User只需要拥有这个几个属性：id,如需消息发送，请提供(email,jabber,sms)
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void assign(Map<String,Set<Long>> userIds, OpenExecution execution) throws Exception {
		
		assign(userIds, execution, null);
		//Long deploymentId = getDeploymentId(execution);
		//Long processInitiatorId = getProcessInitiator(execution);// 这是拿到流程启动者,一般为制单人,此处为userid
	
		//dataPermissionService = env.get(DataPermissionService.class);
		//userService = env.get(UserService.class);
		//staffService = env.get(StaffService.class);
		
		//processService = env.get(ProcessService.class);
		//Deployment deployment = processService.getDeployment(deploymentId);
		//deployment.gete
		//String activeCode = execution.getActivity().getName();
		//String processKey = deployment.getProcessKey();
		//int processVersion = deployment.getProcessVersion();
		
		
	/*	String groupIds = "";
		if (execution.getGroupEnabled() != null && execution.getGroupEnabled())
			groupIds = getInitiatorGroups(execution);
		Boolean crossCompanyFlag=false;
		if(execution.getCrossCompanyFlag()!=null&&execution.getCrossCompanyFlag()){
			crossCompanyFlag=true;
		}
		List<Long> userList = dataPermissionService.getPowerUserList(processInitiatorId, activeCode, processKey, String.valueOf(processVersion), getInitiatorPositionId(execution),
				groupIds,crossCompanyFlag);
		if (null == userList || userList.isEmpty())
			userList = new ArrayList<Long>();
		User inputUser = userService.load(processInitiatorId);
		Staff staff;
		
		if (inputorFlag != null && inputorFlag.equals("true")) {
			if (inputUser != null) {
				userList.add(inputUser.getId());
				// assignorList.add(new AssignUser(inputUser.getName()));
			}
		}
		if (leaderFlag != null && leaderFlag.equals("true")) {
			staff = inputUser.getStaff();
			if (staff.getLeaderStaffId() != null) {

				Staff leaderStaff = staffService.load(staff.getLeaderStaffId());
				if (null != leaderStaff) {
					User leaderUser = leaderStaff.getUser();
					userList.add(leaderUser.getId());
				}
			}
		}
		if (bigLeaderFlag != null && bigLeaderFlag.equals("true")) {
			staff = inputUser.getStaff();
			if (staff.getHigherLeaderStaffId() != null) {
				Staff HigherloaderStaff = staffService.load(staff.getHigherLeaderStaffId());
				if (HigherloaderStaff != null) {
					User higherLeaderUser = HigherloaderStaff.getUser();
					if (null != higherLeaderUser) {
						userList.add(higherLeaderUser.getId());
					}
				}
			}
		}

		 //指定人员实现 
		// /////////////////////////////////////////////////////////////////
		ExecutionImpl ex=(ExecutionImpl)execution;
		if(null!=ex.getWorkFlowVar()){
			Set<Long> assignUsers=ex.getWorkFlowVar().getAdditionalUsers();
			if (null != assignUsers && assignUsers.size() > 0) {
				userList.addAll(assignUsers);
			}
		}
		
		//候选人
		if (null != staffIds && staffIds.trim().length() > 0) {
			// 先解析出来
			Map<String, Object> variables = Variables.executeAll(execution);
			String[] staffArr=staffIds.split(",");
			for(String staffIdsTemp:staffArr){
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
		
	
		//预期委托功能-
		Map<Long, List<Long>> expectedConsignor = taskService.getAssigeUser(processKey, activeCode);
		List<Long> temp=new ArrayList<Long>();
		for(Long pendingUserId:userList){
			if(expectedConsignor.get(pendingUserId)!=null){
				temp.addAll(expectedConsignor.get(pendingUserId));
			}
		}
		if(!temp.isEmpty()){
			userList.addAll(temp);
		}
		userIds.addAll(userList);*/
			}

	public void setInputorFlag(String inputorFlag) {
		this.inputorFlag = inputorFlag;
	}

	public void setLeaderFlag(String leaderFlag) {
		this.leaderFlag = leaderFlag;
	}

	public void setBigLeaderFlag(String bigLeaderFlag) {
		this.bigLeaderFlag = bigLeaderFlag;
	}

	public void setStaffIds(String staffIds) {
		this.staffIds = staffIds;
	}

	public String getInputorFlag() {
		return inputorFlag;
	}

	public String getLeaderFlag() {
		return leaderFlag;
	}

	public String getBigLeaderFlag() {
		return bigLeaderFlag;
	}

	public String getStaffIds() {
		return staffIds;
	}

	public String getFlowDealFlag() {
		return flowDealFlag;
	}

	public void setFlowDealFlag(String flowDealFlag) {
		this.flowDealFlag = flowDealFlag;
	}

	public String getActivityDealFlag() {
		return activityDealFlag;
	}

	public void setActivityDealFlag(String activityDealFlag) {
		this.activityDealFlag = activityDealFlag;
	}

	public String getAttentFlag() {
		return attentFlag;
	}

	public void setAttentFlag(String attentFlag) {
		this.attentFlag = attentFlag;
	}

}