/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.listener;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.event.impl.FlowableProcessStartedEventImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.supcon.supfusion.flow.common.util.Constants;

/**
 * @author: zhuangmh
 * @date: 2020年11月4日 下午3:55:56
 */
public abstract class AbstractProcessStartListener extends AbstractFlowableEventListener {
    
    @Autowired
    private RepositoryService repositoryService;
    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#onEvent(org.flowable.common.engine.api.delegate.event.FlowableEvent)
     */
    @Override
    public void onEvent(FlowableEvent event) {
        FlowableProcessStartedEventImpl startEvent = (FlowableProcessStartedEventImpl)event;
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(startEvent.getProcessDefinitionId()).singleResult();
        if (pd != null) {
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(pd.getDeploymentId()).singleResult();
            String processName = (String)startEvent.getVariables().get(Constants.PROCESS_NAME);
            if (processName == null) {
                processName = pd.getName();
            }
            createProcess(startEvent.getProcessInstanceId(), pd.getKey(), processName, Integer.parseInt(deployment.getKey()));
        }
    }

    public abstract void createProcess(String processId, String processKey, String processName, int processVersion);
    
    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#isFailOnException()
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }

}
