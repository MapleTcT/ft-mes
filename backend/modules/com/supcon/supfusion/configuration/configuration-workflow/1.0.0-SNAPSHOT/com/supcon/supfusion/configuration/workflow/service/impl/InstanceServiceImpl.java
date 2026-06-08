package com.supcon.supfusion.configuration.workflow.service.impl;

import com.supcon.supfusion.base.services.InstanceService;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("wf_instanceService")
public class InstanceServiceImpl implements InstanceService {
	@Autowired
	private ExecutionService executionService;
	@Override
	@Transactional
	public void deleteProcessInstance(String key, int version){
		String id = key + "-" + version;
		List<ProcessInstance> list=executionService.createProcessInstanceQuery().processDefinitionId(id).list();
		for(ProcessInstance pc:list){
			executionService.deleteProcessInstance(pc.getId());
		}
		
	}

}
