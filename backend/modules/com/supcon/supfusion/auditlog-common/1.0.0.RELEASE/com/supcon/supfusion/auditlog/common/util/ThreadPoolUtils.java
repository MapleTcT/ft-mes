package com.supcon.supfusion.auditlog.common.util;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.supcon.supfusion.framework.cloud.common.thread.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 */
public class ThreadPoolUtils {

    private static final ExecutorService executorService = TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(
            4,
            4,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1),
            new NamedThreadFactory("auditlog-worker"),
            new ThreadPoolExecutor.AbortPolicy()));


    public static ExecutorService getThreadPool() {
        return executorService;
    }
}
