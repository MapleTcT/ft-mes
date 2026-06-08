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

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.services.StaffService;
import com.supcon.supfusion.configuration.services.entity.EntityTableInfo;
import com.supcon.supfusion.configuration.workflow.entities.IModelStatus;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import org.jbpm.api.activity.ActivityBehaviour;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.model.Activity;
import org.jbpm.api.model.Transition;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Tom Baeyens
 */
public class EndActivity extends AbstractNoticeActivity implements ActivityBehaviour {
	private static final Logger logger = LoggerFactory.getLogger(EndActivity.class);

	private static final long serialVersionUID = 1L;

	protected boolean endProcessInstance = true;
	protected String state = null;
	protected boolean effect;

	public void execute(ActivityExecution execution) {
		ExecutionImpl exc=(ExecutionImpl) execution;
		execute(exc);
		String transitionName="";
		if(exc.getTransition()!=null){
			transitionName=exc.getTransition().getName();
		}else if(exc.getWorkFlowVar()!=null){
			transitionName=exc.getWorkFlowVar().getOutcome();
		}else{
			transitionName=exc.getActivity().getOutgoingTransitions().get(0).getName();
		}
		exc.historyAutomatic(transitionName);
		
	}

	public void execute(ExecutionImpl execution) {
		Activity activity = execution.getActivity();
		List<? extends Transition> outgoingTransitions = activity.getOutgoingTransitions();
		ActivityImpl parentActivity = (ActivityImpl) activity.getParentActivity();
		ExecutionImpl exc=(ExecutionImpl) execution;
		//exc.historyActivityEnd();
		if ((parentActivity != null) && ("group".equals(parentActivity.getType()))) {
			// if the end activity itself has an outgoing transition
			// (such end activities should be drawn on the border of the group)
			if ((outgoingTransitions != null) && (outgoingTransitions.size() == 1)) {
				Transition outgoingTransition = outgoingTransitions.get(0);
				// taking the transition that goes over the group boundaries will
				// destroy the scope automatically (see atomic operation TakeTransition)
				execution.take(outgoingTransition);

			} else {
				execution.setActivity(parentActivity);
				execution.signal();
			}

		} else {
			ExecutionImpl executionToEnd = null;
			if (endProcessInstance) {
				executionToEnd = execution.getProcessInstance();
				executionToEnd.setActivity(execution.getActivity());
			} else {
				executionToEnd = execution;
			}

			if (state == null) {
				executionToEnd.end();
			} else {
				executionToEnd.end(state);
			}
		}
		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		WorkflowTaskService taskService = env.get(WorkflowTaskService.class);
		StaffService staffService = env.get(StaffService.class);
		if (null == taskService)
			throw new EcException("could not find TaskService.");
		/* 结束处理 */
		IModelStatus status = null;
		if (null != execution.getWorkFlowVar())
			status = execution.getWorkFlowVar().getStatus();
		
		String beanName = ((ExecutionImpl) execution).getScriptExcuteBeanName();

		// 删除工作流状态信息
		taskService.deleteFlowCurrentStatus(execution);

		if ("cancel".equals(state)) {
			// 作废
			//
			if (null != status)
				status.setStatus(EntityTableInfo.STATUS_INVALID);
			
			
			taskService.invalid(execution);
			
			
			// variables.put("springContext", OrchidUtils.getSpringContext(bundle));
//			IScriptService iss = (IScriptService) bundleContext.getService(refs[0]);
//			if (null != iss) {
//				iss.syncEntity(execution.getModelId(), "cancel");
//			}  else
//				throw new BAPException("could not found IScriptService");

		} else {
			// 回填生效
			if (effect) {
				taskService.effect(execution);
			}
			
			if (null != status)
				status.setStatus(EntityTableInfo.STATUS_EFFECTED);
			
			// 消息提醒
			Staff s = staffService.get(execution.getOwnerId());
			Set<Long> userIds = new HashSet<Long>();
			if(s.getUser()!=null){
				userIds.add(s.getUser().getId());
				sendNotice(execution, userIds);// 发消息
				//taskService.effect(execution);
			}
			
		}
	}

	public void setEndProcessInstance(boolean endProcessInstance) {
		this.endProcessInstance = endProcessInstance;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setEffect(boolean effect) {
		this.effect = effect;
	}

}
