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

import org.jbpm.api.history.HistoryActivityInstance;
import org.jbpm.api.history.HistoryProcessInstance;
import org.jbpm.pvm.internal.id.DbidGenerator;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;

import java.io.Serializable;
import java.util.*;

/** base activity instance class.
 *  
 * @author Tom Baeyens
 */
public class HistoryActivityInstanceImpl implements HistoryActivityInstance, Serializable {

  private static final long serialVersionUID = 1L;

  protected Long dbid;
  protected int dbversion;

  protected HistoryProcessInstance historyProcessInstance;
  protected String executionId;
  protected ActivityImpl activity;
  protected String type;
  protected String activityName;

  protected Date startTime;
  protected Date endTime;
  protected long duration;
  
  protected String transitionName;
  protected Long tableInfoId;
  protected int nextDetailIndex = 1;
  protected Long creatorId;
  protected Long dealerId;
  protected Long departmentId;
  protected Long positionId;
  /** only here to get hibernate cascade */
  protected Set<HistoryDetailImpl> details = new HashSet<HistoryDetailImpl>();

  
  public HistoryActivityInstanceImpl() {
  }

  public HistoryActivityInstanceImpl(HistoryProcessInstance historyProcessInstanceImpl, ExecutionImpl execution) {
    this.historyProcessInstance = historyProcessInstanceImpl;
    this.activity = execution.getActivity();
    if(execution.getTransition()!=null){
    	this.setTransitionName(execution.getTransition().getName());
    }
    else if(execution.getWorkFlowVar()!=null&&execution.getWorkFlowVar().getOutcome()!=null){
    	this.setTransitionName(execution.getWorkFlowVar().getOutcome());
    }
   
    ///this.creatorId=getCreatorId();
    //this.dealerId=getCreatorId();
    this.executionId = execution.getId();
    this.activityName = activity.getName();
    this.startTime = execution.getHistoryActivityStart();
    this.dbid = DbidGenerator.getNextId("JBPM4_HIST_ACTINST",1, null);
    this.tableInfoId=execution.getTableInfoId();
  }
  
  
  // details //////////////////////////////////////////////////////////////////
  
  public Long getDepartmentId() {
	return departmentId;
}

public Long getTableInfoId() {
	return tableInfoId;
}

public void setTableInfoId(Long tableInfoId) {
	this.tableInfoId = tableInfoId;
}

public void setDepartmentId(Long departmentId) {
	this.departmentId = departmentId;
}

public Long getPositionId() {
	return positionId;
}

public void setPositionId(Long positionId) {
	this.positionId = positionId;
}

public void addDetail(HistoryDetailImpl detail) {
    detail.setHistoryActivityInstance(this, nextDetailIndex);
    nextDetailIndex++;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
    this.duration = (endTime.getTime() - startTime.getTime())/1000;
  }
  
  public Long getDbid() {
    return dbid;
  }
  public ActivityImpl getActivity() {
    return activity;
  }
  public String getActivityName() {
    return activityName;
  }
  public Date getStartTime() {
    return startTime;
  }
  public Date getEndTime() {
    return endTime;
  }
  public long getDuration() {
    return duration;
  }
  public HistoryProcessInstance getHistoryProcessInstance() {
    return historyProcessInstance;
  }
  public String getExecutionId() {
    return executionId;
  }
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }
  public String getTransitionName() {
    return transitionName;
  }
  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  
  public Long getCreatorId() {
	return creatorId;
}

public void setCreatorId(Long creatorId) {
	this.creatorId = creatorId;
}

public Long getDealerId() {
	return dealerId;
}

public void setDealerId(Long dealerId) {
	this.dealerId = dealerId;
}

public int getDbversion() {
	return dbversion;
}

public void setDbversion(int dbversion) {
	this.dbversion = dbversion;
}

public int getNextDetailIndex() {
	return nextDetailIndex;
}

public void setNextDetailIndex(int nextDetailIndex) {
	this.nextDetailIndex = nextDetailIndex;
}

public Set<HistoryDetailImpl> getDetails() {
	return details;
}

public void setDetails(Set<HistoryDetailImpl> details) {
	this.details = details;
}

public void setDbid(Long dbid) {
	this.dbid = dbid;
}

public void setHistoryProcessInstance(
		HistoryProcessInstance historyProcessInstance) {
	this.historyProcessInstance = historyProcessInstance;
}

public void setActivity(ActivityImpl activity) {
	this.activity = activity;
}

public void setActivityName(String activityName) {
	this.activityName = activityName;
}

public void setStartTime(Date startTime) {
	this.startTime = startTime;
}

public void setDuration(long duration) {
	this.duration = duration;
}


public List<String> getTransitionNames() {
    // TODO: expand for multiple outgoing transitions.
    // Currently not possible, since only one transition name is stored.
    if (transitionName != null) {
      List<String> transitionNames = new ArrayList<String>();
      transitionNames.add(transitionName);
      return transitionNames;
    }
    return Collections.emptyList();
  }
}
