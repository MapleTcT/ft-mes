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
package org.jbpm.pvm.internal.cmd;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.workflow.entities.WorkFlowVar;
import org.hibernate.Session;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.client.ClientProcessDefinition;
import org.jbpm.pvm.internal.client.ClientProcessInstance;
import org.jbpm.pvm.internal.session.RepositorySession;

import java.util.Map;

/**
 * @author Tom Baeyens
 */
public class StartProcessInstanceCmd extends VariablesCmd<ProcessInstance> {

  private static final long serialVersionUID = 1L;
  
  protected String processDefinitionId;
  protected String executionKey;
  
//  protected Long processInitiator;
//  protected Long tableInfoId;
//  protected Long deploymentId;
//  protected Long entityId;
  protected WorkFlowVar workFlowVar;
  

  public StartProcessInstanceCmd(String processDefinitionId, Map<String, ?> variables, String executionKey) {
    this.processDefinitionId = processDefinitionId;
    this.variables = variables;
    this.executionKey = executionKey;
  }
  
	public StartProcessInstanceCmd(String processDefinitionId, Map<String, ?> variables, String executionKey, WorkFlowVar workFlowVar) {
		this(processDefinitionId, variables, executionKey);
		this.workFlowVar = workFlowVar;
	}

  public ProcessInstance execute(Environment environment) throws Exception {
    RepositorySession repositorySession = environment.get(RepositorySession.class);

    ClientProcessDefinition processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
    if (processDefinition==null) {
    	
//      throw new JbpmException("no process definition with id '"+processDefinitionId+"'");
    	throw new JbpmException(InternationalResource.get("ec.workflow.noProcessDefinition"));
    }
    
    ClientProcessInstance processInstance = processDefinition.createProcessInstance(executionKey);
    processInstance.setVariables(variables);
    
    ////////////////////////////////////////自定义////////////////////////////////////////
		if (null != workFlowVar) {
			processInstance.setProcessInitiator(workFlowVar.getProcessInitiator());
			processInstance.setTableInfoId(workFlowVar.getTableInfoId());
			processInstance.setDeploymentId(workFlowVar.getDeploymentId());
//			processInstance.setEntityId(workFlowVar.getEntityId());
			processInstance.setEntityCode(workFlowVar.getEntityCode());
			processInstance.setOwnerId(workFlowVar.getOwnerId());
			processInstance.setOwnerPositionId(workFlowVar.getOwnerPositionId());
			processInstance.setTableName(workFlowVar.getTableName());
			processInstance.setInitiatorPositionId(workFlowVar.getInitiatorPositionId());
			processInstance.setModelId(workFlowVar.getModelId());
			processInstance.setTableNo(workFlowVar.getTableNo());
			processInstance.setGroupEnabled(workFlowVar.getGroupEnabled());
			processInstance.setCrossCompanyFlag(workFlowVar.getCrossCompanyFlag());
			processInstance.setScriptExcuteBeanName(workFlowVar.getScriptExcuteBeanName());
			processInstance.setAssignUsers(workFlowVar.getAdditionalUsers());
			processInstance.setOperateType(workFlowVar.getOperateType());
			processInstance.setWorkFlowVar(workFlowVar);
		}
    ////////////////////////////////////////自定义////////////////////////////////////////
    
    processInstance.start();
    
   

    if (!processInstance.isEnded()) {
      Session session = environment.get(Session.class);
      session.save(processInstance);
    }

    return processInstance;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionId;
  }
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionId = processDefinitionKey;
  }
  public String getExecutionKey() {
    return executionKey;
  }
  public void setExecutionKey(String executionKey) {
    this.executionKey = executionKey;
  }
}
