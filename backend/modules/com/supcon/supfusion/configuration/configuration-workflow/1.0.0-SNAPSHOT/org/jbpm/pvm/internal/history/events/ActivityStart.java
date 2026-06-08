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
package org.jbpm.pvm.internal.history.events;

import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import org.jbpm.api.history.HistoryProcessInstance;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.history.HistoryEvent;
import org.jbpm.pvm.internal.history.model.HistoryActivityInstanceImpl;
import org.jbpm.pvm.internal.history.model.HistoryProcessInstanceImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * @author Tom Baeyens
 */
public class ActivityStart extends HistoryEvent {

  private static final long serialVersionUID = 1L;
  protected String transitionName;
  public String getTransitionName() {
    return transitionName;
  }
  public ActivityStart() {
  }

  public ActivityStart(String transitionName) {
    this.transitionName = transitionName;
  }
  public void process() {
    DbSession dbSession = EnvironmentImpl.getFromCurrent(DbSession.class);

    long processInstanceDbid = execution.getProcessInstance().getDbid();

    HistoryProcessInstance historyProcessInstanceImpl = dbSession.get(HistoryProcessInstanceImpl.class, processInstanceDbid);
    HistoryActivityInstanceImpl historyActivityInstance = 
        createHistoryActivityInstance(historyProcessInstanceImpl);
    SecurityContext securityContext = SecurityContextHolder.getContext();
    if(securityContext!=null){
    	OrchidAuthenticationToken orchidAuth=(OrchidAuthenticationToken) securityContext.getAuthentication();
    	if(orchidAuth!=null&&orchidAuth.getCurrentUser()!=null){
    		historyActivityInstance.setCreatorId(orchidAuth.getCurrentUser().getId());
    	}else{
    		historyActivityInstance.setCreatorId(getExecution().getProcessInitiator());
    	}
    	
    }
    String activityType = execution.getActivity().getType();
    historyActivityInstance.setType(activityType);
    if(transitionName!=null){
    	historyActivityInstance.setTransitionName(transitionName);
    }
    dbSession.save(historyActivityInstance);
   
    
    execution.setHistoryActivityInstanceDbid(historyActivityInstance.getDbid());
  }

  protected HistoryActivityInstanceImpl createHistoryActivityInstance(HistoryProcessInstance historyProcessInstanceImpl) {
    return new HistoryActivityInstanceImpl(historyProcessInstanceImpl, execution);
  }
}
