package com.supcon.supfusion.configuration.workflow.handlers;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.session.DbSession;

import java.util.List;

public abstract class AbstractAssignmentHandler implements AssignmentHandler {

	private static final long serialVersionUID = 3303530209399983669L;

	/**
	 * 制单人UserId
	 * 
	 * @param execution
	 * @return
	 */
	protected Long getProcessInitiator(OpenExecution execution) {
		return execution.getProcessInitiator();
	}

	protected Long getOwnerId(OpenExecution execution) {
		return execution.getOwnerId();
	}

	protected Long getOwnerPositionId(OpenExecution execution) {
		return execution.getOwnerPositionId();
	}

	protected Long getDeploymentId(OpenExecution execution) {
		return execution.getDeploymentId();
	}

	/**
	 * 制单岗位
	 * 
	 * 
	 * @return
	 */
	protected Long getInitiatorPositionId(OpenExecution execution) {
		return execution.getOwnerPositionId();
	}

	/**
	 * 以逗号隔开
	 * 
	 * @param execution
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected String getInitiatorGroups(OpenExecution execution) {
		Long tableInfoId = execution.getTableInfoId();
		StringBuilder ids = new StringBuilder();
		DbSessionImpl session = (DbSessionImpl) getEnv().get(DbSession.class);
		try {
			List list = session.getSession().createSQLQuery("SELECT BAP_GROUP_ID FROM " + DbUtils.getGroupTable(execution.getTableName()) + " WHERE TABLE_INFO_ID = ?").setLong(0, tableInfoId).list();
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

	protected Long[] getInitiatorGroupIds(OpenExecution execution) {
		String ids = getInitiatorGroups(execution);
		if (null != ids && ids.trim().length() > 0) {
			String[] ss = ids.split(",");
			Long[] groupIds = new Long[ss.length];
			for (int i = 0; i < ss.length; i++) {
				groupIds[i] = Long.parseLong(ss[i]);
			}
			return groupIds;
		}
		return new Long[0];
	}

	protected EnvironmentImpl getEnv() {
		return EnvironmentImpl.getCurrent();
	}
}
