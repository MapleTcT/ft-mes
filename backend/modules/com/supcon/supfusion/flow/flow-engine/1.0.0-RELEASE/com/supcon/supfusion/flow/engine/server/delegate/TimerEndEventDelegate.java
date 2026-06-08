/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;

/**
 * @author: zhuangmh
 * @date: 2021年1月19日 下午7:35:12
 */
@Component("timerEndEventDelagate")
public class TimerEndEventDelegate implements JavaDelegate {
    
    /**
     * @see org.flowable.engine.delegate.JavaDelegate#execute(org.flowable.engine.delegate.DelegateExecution)
     */
    @Override
    public void execute(DelegateExecution execution) {
        // 由于定时器是异步执行, 下游环节将无法获取租户ID
        // 因此需要在定时任务结束前将租户ID设置到当前线程上下文
        RpcContext.getContext().setTenantId(execution.getTenantId());
    }
}
