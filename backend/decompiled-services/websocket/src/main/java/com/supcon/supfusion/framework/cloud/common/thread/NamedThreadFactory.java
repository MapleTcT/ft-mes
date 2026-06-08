/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory
implements ThreadFactory {
    private final AtomicInteger threadNum = new AtomicInteger(1);
    private final String prefix;
    private final boolean daemon;
    private final ThreadGroup threadGroup;
    private final Thread.UncaughtExceptionHandler eh;

    public NamedThreadFactory() {
        this("fusion");
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        this(prefix, daemon, null);
    }

    public NamedThreadFactory(String prefix, boolean daemon, Thread.UncaughtExceptionHandler eh) {
        this.prefix = prefix + "-thread-";
        this.daemon = daemon;
        this.eh = eh;
        SecurityManager s = System.getSecurityManager();
        this.threadGroup = s == null ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread result = new Thread(this.threadGroup, r, this.prefix + this.threadNum.getAndIncrement(), 0L);
        result.setDaemon(this.daemon);
        if (this.eh != null) {
            result.setUncaughtExceptionHandler(this.eh);
        }
        return result;
    }
}

