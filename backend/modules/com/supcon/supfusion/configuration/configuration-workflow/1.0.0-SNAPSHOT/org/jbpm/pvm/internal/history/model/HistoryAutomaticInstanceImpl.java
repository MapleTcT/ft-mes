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
package org.jbpm.pvm.internal.history.model;

import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.UserService;
import org.jbpm.api.history.HistoryProcessInstance;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.util.Clock;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * @author Tom Baeyens
 */
public class HistoryAutomaticInstanceImpl extends HistoryActivityInstanceImpl {
  
  private static final long serialVersionUID = 1L;

  public HistoryAutomaticInstanceImpl() {
  }

  public HistoryAutomaticInstanceImpl(HistoryProcessInstance historyProcessInstanceImpl, ExecutionImpl execution) {
    super(historyProcessInstanceImpl, execution);
    setEndTime(Clock.getTime());
    SecurityContext securityContext = SecurityContextHolder.getContext();
    if(securityContext!=null){
    	
    	OrchidAuthenticationToken orchidAuth=(OrchidAuthenticationToken) securityContext.getAuthentication();
    	if(orchidAuth!=null&&orchidAuth.getCurrentUser()!=null){
    		User user=(User)orchidAuth.getCurrentUser();
        	setDealerId(user.getId());
        	setCreatorId(user.getId());
        	Staff staff=user.getStaff();
        	Position position=(Position)staff.getMainPosition();
        	Department department=position.getDepartment();
        	setDepartmentId(department.getId());
        	setPositionId(position.getId());
    	}else{
    		EnvironmentImpl env = EnvironmentImpl.getCurrent();
    		UserService userService = env.get(UserService.class);
    		
    		Long createStaffId=execution.getProcessInitiator();
        	User user=userService.load(createStaffId);
        	if(user!=null){
        		setDealerId(user.getId());
    	    	setCreatorId(user.getId());
        		Staff staff=user.getStaff();
        		if(staff!=null){
        	    	Position position=(Position)staff.getMainPosition();
        	    	Department department=position.getDepartment();
        	    	setDepartmentId(department.getId());
        	    	setPositionId(position.getId());
        		}
        	}
    		
    	}
    	
    }
   
  }
}
