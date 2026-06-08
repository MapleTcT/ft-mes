package com.supcon.supfusion.auth.common.utils;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.supcon.supfusion.framework.cloud.common.thread.NamedThreadFactory;

import java.util.concurrent.*;

/**
 * 线程池
 */
public class ThreadPoolUtils {

    private static final ExecutorService executorService = TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(
            4,
            4,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new NamedThreadFactory("excel导入导出"),
            new ThreadPoolExecutor.AbortPolicy()));

    public static final ScheduledExecutorService onlineUserService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("onlineUserTask"));

    public static ExecutorService getThreadPool() {
        return executorService;
    }
}
