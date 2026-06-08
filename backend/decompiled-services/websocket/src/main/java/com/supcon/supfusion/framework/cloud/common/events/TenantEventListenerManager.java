/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.BeansException
 *  org.springframework.context.ApplicationContext
 *  org.springframework.context.ApplicationContextAware
 *  org.springframework.context.ApplicationEvent
 *  org.springframework.context.ApplicationListener
 *  org.springframework.lang.NonNull
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventListener;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.cloud.common.thread.ConcurrentTreeSet;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

public class TenantEventListenerManager
implements ApplicationListener<ApplicationEvent>,
ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(TenantEventListenerManager.class);
    private static final ConcurrentTreeSet<TenantEventListener> LISTENERS = new ConcurrentTreeSet<TenantEventListener>(Comparator.comparingInt(TenantEventListener::order));
    private static volatile boolean doListen = true;

    public void addListener(@NonNull TenantEventListener listener) {
        LISTENERS.add(listener);
    }

    public void removeListener(@NonNull TenantEventListener listener) {
        LISTENERS.remove(listener);
    }

    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof TenantAddEvent) {
            TenantAddEvent addEvent = (TenantAddEvent)event;
            log.info("on tenant add event, id={}", (Object)addEvent.getTenant().getId());
            TenantInfoLocalStorage.add(addEvent.getTenant());
            if (doListen) {
                LISTENERS.forEach(l -> {
                    try {
                        l.onAdd(addEvent);
                    }
                    catch (Throwable e) {
                        log.warn("deal tenant add event failed", e);
                    }
                });
            }
        } else if (event instanceof TenantDestroyEvent) {
            TenantDestroyEvent destroyEvent = (TenantDestroyEvent)event;
            log.info("on tenant destroy event, id={}", (Object)destroyEvent.getTenant().getId());
            if (doListen) {
                LISTENERS.forEach(l -> {
                    try {
                        l.onDestroy(destroyEvent);
                    }
                    catch (Throwable e) {
                        log.warn("deal tenant destroy event failed", e);
                    }
                });
            }
            TenantInfoLocalStorage.destroy(destroyEvent.getTenant().getId());
        }
    }

    public static void onAdd(TenantInfo tenantInfo) {
        if (log.isDebugEnabled()) {
            log.info("beginning to deal tenant add event, tenantInfo={}", (Object)tenantInfo);
        }
        TenantInfoLocalStorage.add(tenantInfo);
        if (doListen) {
            if (log.isDebugEnabled()) {
                log.info("execute tenant add event listeners");
            }
            TenantAddEvent event = new TenantAddEvent(new Object(), tenantInfo);
            LISTENERS.forEach(l -> {
                try {
                    l.onAdd(event);
                }
                catch (Throwable e) {
                    log.warn("deal tenant add event failed", e);
                }
            });
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }
}

