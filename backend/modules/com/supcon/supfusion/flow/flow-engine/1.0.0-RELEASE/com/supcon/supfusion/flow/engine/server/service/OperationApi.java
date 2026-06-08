/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service;

import java.util.Set;

import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:35:53
 */
public interface OperationApi {
    
    /**
     * 重置待办执行者
     * @param delegateTask
     * @return 返回最新的执行者列表
     */
    Set<String> changeRecipient(DelegateTask delegateTask);
}
