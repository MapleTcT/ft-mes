/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.supcon.supfusion.configuration.workflow.activity;

import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.model.Activity;
import org.jbpm.api.model.Transition;
import org.jbpm.jpdl.internal.activity.JpdlActivity;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Tom Baeyens
 */
public class JoinActivity extends JpdlActivity {

	private static final long serialVersionUID = 1L;

	private LockMode lockMode = LockMode.UPGRADE;
	private Integer multiplicity;
	
	private static final Logger LOG = LoggerFactory.getLogger(JoinActivity.class);

	public void execute(ActivityExecution execution) {
		execute((ExecutionImpl) execution);
	}

	public void execute(ExecutionImpl execution) {

		// 经过聚合活动后,待办不可撤回
		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		WorkflowTaskService taskService = env.get(WorkflowTaskService.class);
		taskService.setRecallAbleFlag(false);

		ActivityImpl activity = execution.getActivity();

		// if this is a single, non concurrent root
		if (Execution.STATE_ACTIVE_ROOT.equals(execution.getState())) {
			// just pass through
			Transition transition = activity.getDefaultOutgoingTransition();
			if (transition == null) {
				throw new JbpmException("join must have an outgoing transition");
			}
			execution.take(transition);
			//STATE_ACTIVE_CONCURRENT--并发过程中
		} else if (Execution.STATE_ACTIVE_CONCURRENT.equals(execution.getState())) {

			// force version increment in the parent execution
			Session session = EnvironmentImpl.getFromCurrent(Session.class);
			session.lock(execution.getParent(), lockMode);

			execution.setState(Execution.STATE_INACTIVE_JOIN);
			execution.waitForSignal();

			ExecutionImpl concurrentRoot = execution.getParent();
			List<ExecutionImpl> joinedExecutions = getJoinedExecutions(concurrentRoot, activity);

			if (isComplete(execution, joinedExecutions)) {
				endExecutions(joinedExecutions);
				// if multiplicity was used
				if (multiplicity != null) {
					// collect concurrent executions still active
					List<ExecutionImpl> danglingExecutions = new ArrayList<ExecutionImpl>();
					for (ExecutionImpl concurrentExecution : concurrentRoot.getExecutions()) {
						//先测试一下，已经通过的也要end--------只要聚合结束，就把分发其他分支实例结束点
						//if (Execution.STATE_ACTIVE_CONCURRENT.equals(concurrentExecution.getState())) {
							danglingExecutions.add(concurrentExecution);
						//}
					}
					// end dangling executions
					endExecutions(danglingExecutions);
				}
				ExecutionImpl outgoingExecution = null;
				if (concurrentRoot.getExecutions().isEmpty()) {
					outgoingExecution = concurrentRoot;
					outgoingExecution.setState(Execution.STATE_ACTIVE_ROOT);
				} else {
					outgoingExecution = concurrentRoot.createExecution();
					outgoingExecution.setState(Execution.STATE_ACTIVE_CONCURRENT);
				}
				
				
				outgoingExecution.setTableInfoId(concurrentRoot.getTableInfoId());
//				outgoingExecution.setEntityId(concurrentRoot.getEntityId());
				outgoingExecution.setEntityCode(concurrentRoot.getEntityCode());
				outgoingExecution.setDeploymentId(concurrentRoot.getDeploymentId());
				outgoingExecution.setInitiatorPositionId(concurrentRoot.getInitiatorPositionId());
				outgoingExecution.setModelId(concurrentRoot.getModelId());
				outgoingExecution.setOwnerId(concurrentRoot.getOwnerId());
				outgoingExecution.setOwnerPositionId(concurrentRoot.getOwnerPositionId());
				outgoingExecution.setProcessInitiator(concurrentRoot.getProcessInitiator());
				outgoingExecution.setTableNo(concurrentRoot.getTableNo());
				outgoingExecution.setGroupEnabled(concurrentRoot.getGroupEnabled());
				outgoingExecution.setTableName(concurrentRoot.getTableName());
				outgoingExecution.setVariablesProvider(concurrentRoot.getVariablesProvider());
				

				execution.setActivity(activity, outgoingExecution);
				Transition transition = activity.getDefaultOutgoingTransition();
				if (transition == null) {
					throw new JbpmException("join must have an outgoing transition");
				}
				outgoingExecution.take(transition);
			}

		} else {
			throw new JbpmException("invalid execution state");
		}
	}

	protected boolean isComplete(ExecutionImpl execution, List<ExecutionImpl> joinedExecutions) {
		int executionsToJoin;
		if (multiplicity != null) {
			// executionsToJoin = evaluateMultiplicity(execution);
			executionsToJoin = multiplicity;
		} else {
			executionsToJoin = execution.getParent().getExecutions().size();
			LOG.info("fork executions count:" + executionsToJoin);
		}
		return joinedExecutions.size() == executionsToJoin;
	}

	protected List<ExecutionImpl> getJoinedExecutions(ExecutionImpl concurrentRoot, Activity activity) {
		List<ExecutionImpl> joinedExecutions = new ArrayList<ExecutionImpl>();
		Collection<ExecutionImpl> concurrentExecutions = concurrentRoot.getExecutions();
		for (ExecutionImpl concurrentExecution : concurrentExecutions) {
			if ((Execution.STATE_INACTIVE_JOIN.equals(concurrentExecution.getState())) && (concurrentExecution.getActivity() == activity)) {
				joinedExecutions.add(concurrentExecution);
			}
		}
		return joinedExecutions;
	}

	protected void endExecutions(List<ExecutionImpl> executions) {
		for (ExecutionImpl execution : executions) {
			execution.end();
		}
	}

	// private int evaluateMultiplicity(ExecutionImpl execution) {
	// if (multiplicity != null) {
	// Object value = multiplicity.evaluate(execution);
	// if (value instanceof Number) {
	// Number number = (Number) value;
	// return number.intValue();
	// }
	// if (value instanceof String) {
	// return Integer.parseInt((String) value);
	// }
	// }
	// return -1;
	// }

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	//
	// public void setMultiplicity(Expression multiplicity) {
	// this.multiplicity = multiplicity;
	// }

	public void setMultiplicity(Integer multiplicity) {
		this.multiplicity = multiplicity;
	}

}
