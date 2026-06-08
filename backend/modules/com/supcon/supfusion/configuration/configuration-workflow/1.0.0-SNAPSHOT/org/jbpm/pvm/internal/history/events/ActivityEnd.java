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
import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.User;
import org.hibernate.Session;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.history.HistoryEvent;
import org.jbpm.pvm.internal.history.model.HistoryActivityInstanceImpl;
import org.jbpm.pvm.internal.util.Clock;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * @author Tom Baeyens
 */
public class ActivityEnd extends HistoryEvent {

  private static final long serialVersionUID = 1L;

  protected String transitionName;
  
  public ActivityEnd() {
  }

  public ActivityEnd(String transitionName) {
    this.transitionName = transitionName;
  }

  public void process() {
    Session session = EnvironmentImpl.getFromCurrent(Session.class);
    Long historyActivityInstanceDbId = execution.getHistoryActivityInstanceDbid();
    if(null!=historyActivityInstanceDbId){
    	 HistoryActivityInstanceImpl historyActivityInstance = (HistoryActivityInstanceImpl) 
    		        session.load(getHistoryActivityInstanceClass(), historyActivityInstanceDbId); 
    		    
	    updateHistoryActivityInstance(historyActivityInstance);
	    session.update(historyActivityInstance);
    }
   
  }

  protected void updateHistoryActivityInstance(HistoryActivityInstanceImpl historyActivityInstance) {
    historyActivityInstance.setEndTime(Clock.getTime());
    SecurityContext securityContext = SecurityContextHolder.getContext();
    if(securityContext!=null){
    	OrchidAuthenticationToken orchidAuth=(OrchidAuthenticationToken) securityContext.getAuthentication();
    	if(orchidAuth!=null){
    		User user=(User)orchidAuth.getCurrentUser();
        	historyActivityInstance.setDealerId(user.getId());
        	Staff staff=user.getStaff();
        	Position position=(Position)staff.getMainPosition();
        	Department department=position.getDepartment();
        	historyActivityInstance.setDepartmentId(department.getId());
        	historyActivityInstance.setPositionId(position.getId());
    	}
    }
    //historyActivityInstance.setTransitionName(transitionName);
  }

  protected Class<? extends HistoryActivityInstanceImpl> getHistoryActivityInstanceClass() {
    return HistoryActivityInstanceImpl.class;
  }
  
  public String getTransitionName() {
    return transitionName;
  }
  
}
