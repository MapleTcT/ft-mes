package com.supcon.supfusion.configuration.workflow.variables;

import com.supcon.supfusion.configuration.workflow.service.VariablesProvider;
import org.jbpm.api.model.OpenExecution;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程变量快捷操作类.
 * 
 * @author songjiawei
 * 
 */
public abstract class Variables {
	/**
	 * 获取除单据业务数据外的变量.这个方法几乎无性能损耗.如果在不用到表单中业务数据时使用此方法.
	 * 
	 * @param execution
	 * @return
	 */
	public static Map<String, Object> execute(OpenExecution execution) {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("deploymentId", execution.getDeploymentId());
//		vars.put("entityId", execution.getEntityId());
		vars.put("entityCode", execution.getEntityCode());
		vars.put("modelId", execution.getModelId());
		vars.put("initiatorPositionId", execution.getInitiatorPositionId());
		vars.put("processInitiator", execution.getProcessInitiator());
//		vars.put("initiatorGroupIds", execution.getInitiatorGroupIds());
		vars.put("ownerId", execution.getOwnerId());
		vars.put("ownerPositionId", execution.getOwnerPositionId());
		vars.put("tableInfoId", execution.getTableInfoId());
		vars.putAll(execution.getVariables());
		return vars;
	}

	/**
	 * 获取包含表单业务数据在内的变量.
	 * 
	 * @param execution
	 * @param provider
	 *            指定业务数据提供实现
	 * @return
	 */
	public static Map<String, Object> execute(OpenExecution execution, VariablesProvider provider) {
		Map<String, Object> vars = execute(execution);
		if (null != execution.getModelId()) {
			vars.putAll(provider.provide(execution.getModelId()));
		}
		return vars;
	}

	/**
	 * 获取包含表单业务数据在内的变量.默认使用execution中绑定的VariablesProvider.
	 * 
	 * @param execution
	 * @return
	 */
	public static Map<String, Object> executeAll(OpenExecution execution) {
		Map<String, Object> vars = execute(execution);
		if (null != execution.getModelId() && null != execution.getVariablesProvider()) {
			vars.putAll(execution.getVariablesProvider().provide(execution.getModelId()));
		}
		return vars;
	}
}