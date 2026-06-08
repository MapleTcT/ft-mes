/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.ttl.TtlRunnable
 */
package com.supcon.supfusion.framework.cloud.common.thread;

import com.alibaba.ttl.TtlRunnable;
import com.supcon.supfusion.framework.cloud.common.thread.NamedThreadFactory;

public class TtlNamedThreadFactory
extends NamedThreadFactory {
    public TtlNamedThreadFactory() {
    }

    public TtlNamedThreadFactory(String prefix) {
        super(prefix);
    }

    public TtlNamedThreadFactory(String prefix, boolean daemon) {
        super(prefix, daemon);
    }

    public TtlNamedThreadFactory(String prefix, boolean daemon, Thread.UncaughtExceptionHandler eh) {
        super(prefix, daemon, eh);
    }

    @Override
    public Thread newThread(Runnable r) {
        return super.newThread((Runnable)TtlRunnable.get((Runnable)r));
    }
}

