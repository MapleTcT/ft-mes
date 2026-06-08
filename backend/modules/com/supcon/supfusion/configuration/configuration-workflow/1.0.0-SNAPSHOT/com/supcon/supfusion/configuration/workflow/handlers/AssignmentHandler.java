package com.supcon.supfusion.configuration.workflow.handlers;

import com.supcon.supfusion.base.entities.Deployment;
import org.jbpm.api.model.OpenExecution;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 待办接收者接口.
 * 
 * @author songjiawei
 * 
 */
public interface AssignmentHandler extends Serializable {
	/**
	 * 
	 * @param staffs
	 * @param execution
	 * @throws Exception
	 */
	void assign(Map<String,Set<Long>> userIds, OpenExecution execution) throws Exception;
	
	void assign(Map<String,Set<Long>> userIds, OpenExecution execution, Map<Long, String> entrust) throws Exception;
	
	Map<Long, List<Long>> getAssigeUser(OpenExecution execution, Deployment deployment);
}