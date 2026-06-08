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
package org.jbpm.pvm.internal.model.op;

import org.jbpm.api.activity.ExternalActivityBehaviour;
import org.jbpm.internal.log.Log;
import org.jbpm.pvm.internal.job.MessageImpl;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;

import java.util.List;
import java.util.Map;

/**
 * @author Tom Baeyens
 */
public class NotificationSignal extends AtomicOperation {

  private static final long serialVersionUID = 1L;

  private static final Log log = Log.getLog(NotificationSignal.class.getName());

  private final String signalName;
  private final Map<String, ?> parameters;

  public NotificationSignal(String signalName, Map<String, ?> parameters) {
    this.signalName = signalName;
    this.parameters = parameters;
  }

  @Override
  public boolean isAsync(ExecutionImpl execution) {
    return false;
  }

  @Override
  public void perform(ExecutionImpl execution) {
	ProcessDefinitionImpl processDefinition = (ProcessDefinitionImpl)execution.getProcessDefinition();
	ActivityImpl  activityImp=processDefinition.getActivity(execution.getActivityName());
	List<TransitionImpl> transitions = (List<TransitionImpl>) activityImp.getOutgoingTransitions();
	for(TransitionImpl transitionImpl:transitions){
		if(transitionImpl.getNotificationType()==1){
			 try {
				 ActivityImpl  activity=transitionImpl.getDestination();
				 ExternalActivityBehaviour activityBehaviour = (ExternalActivityBehaviour) activity
						 .getActivityBehaviour();
			
				activityBehaviour.execute(execution);
				
				
			 } catch (Exception e) {
			 }
		}
	}
	  
 
   
  }
  
  @Override
  public String toString() {
    return "Signal(" + signalName + ')';
  }

  @Override
  public MessageImpl createAsyncMessage(ExecutionImpl execution) {
    return null;
  }
}
