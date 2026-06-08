/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.ttl.TransmittableThreadLocal
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.framework.cloud.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcContextHolder {
    private static Logger logger = LoggerFactory.getLogger(RpcContextHolder.class);
    private static final ThreadLocal<RpcContext> RPC_CONTEXT_THREAD_LOCAL = new TransmittableThreadLocal<RpcContext>(){

        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    public static RpcContext getContext() {
        return RPC_CONTEXT_THREAD_LOCAL.get();
    }

    public static void setContext(RpcContext context) {
        if (context == null) {
            RpcContextHolder.removeContext();
        } else {
            if (logger.isDebugEnabled()) {
                logger.info("=>[framework]create RPC Context");
            }
            RPC_CONTEXT_THREAD_LOCAL.set(context);
        }
    }

    public static void removeContext() {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]remove RPC Context");
        }
        RPC_CONTEXT_THREAD_LOCAL.remove();
    }
}

