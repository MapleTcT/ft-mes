package com.supcon.supfusion.configuration.workflow.service;


import com.supcon.supfusion.base.entities.Deployment;
import org.jbpm.api.ProcessDefinition;

public interface WorkflowProcessService {

	void update(Deployment deployment, String operatePower, String menuOperateStr, String actives, String updatePowerString, String superviseNamesMultiIDs,
				String selectStaffs, String linkRangeChage, String... env);

	void deploy(Deployment preDeployment, Deployment deployment, boolean setToBeCurrent, String operatePower, String menuOperatStr, String actives,
				String updatePowerString, String superviseNamesMultiIDs, String selectStaffs, String linkRangeChage, String... env);

	ProcessDefinition getProcessDefinition(String processDefinitionId);
}
