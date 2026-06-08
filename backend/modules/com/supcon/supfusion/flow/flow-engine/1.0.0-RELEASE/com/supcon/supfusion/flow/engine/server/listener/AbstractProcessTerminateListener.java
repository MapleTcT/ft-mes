/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.listener;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;

/**
 * <p>
 *  A process has been canceled. Dispatched before the process instance is deleted from runtime. 
 *  A process instance can for example be canceled by the API call RuntimeService.deleteProcessInstance
 * </p>
 * @author: zhuangmh
 * @date: 2020年6月16日 上午9:36:33
 */
public abstract class AbstractProcessTerminateListener extends AbstractFlowableEventListener {
    
    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#onEvent(org.flowable.common.engine.api.delegate.event.FlowableEvent)
     */
    @Override
    public void onEvent(FlowableEvent event) {
        FlowableCancelledEvent cancelledEvent = (FlowableCancelledEvent)event;
        updateProcess(cancelledEvent.getProcessInstanceId());
    }
    
    /**
     * 删除流程
     * @param processId 流程实例ID
     */
    public abstract void updateProcess(String processId);
    
    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#isFailOnException()
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }
}
