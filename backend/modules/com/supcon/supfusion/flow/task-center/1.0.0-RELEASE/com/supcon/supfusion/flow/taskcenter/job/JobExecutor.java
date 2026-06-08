/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: zhuangmh
 * @date: 2020年6月10日 上午10:15:00
 */
public abstract class JobExecutor<T> {
    
    protected final ExecutorService JOB_THREAD_POOL = Executors.newFixedThreadPool(2);

    /**
     * 提交要处理的任务
     */
    public abstract void submit(T t);
}
