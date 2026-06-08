/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.ttl.TtlRunnable
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.BeansException
 *  org.springframework.beans.factory.DisposableBean
 *  org.springframework.context.ApplicationContext
 *  org.springframework.context.ApplicationContextAware
 *  org.springframework.util.CollectionUtils
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.alibaba.ttl.TtlRunnable;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventListenerManager;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoGetter;
import com.supcon.supfusion.framework.cloud.common.thread.NamedThreadFactory;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

public class TenantInfoLocalStorage
implements DisposableBean,
ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(TenantInfoLocalStorage.class);
    private static final Map<String, TenantInfo> TENANT_INFO_MAP = new ConcurrentHashMap<String, TenantInfo>();
    private static AtomicBoolean isInit = new AtomicBoolean(false);
    private static volatile ApplicationContext applicationContext;
    private volatile boolean exit = false;
    private Thread monitor;
    private static final NamedThreadFactory THREAD_FACTORY;

    public static void add(TenantInfo tenantInfo) {
        TENANT_INFO_MAP.put(tenantInfo.getId(), tenantInfo);
        if (log.isDebugEnabled()) {
            log.info("add tenant information to local storage, id={}, size={}", (Object)tenantInfo.getId(), (Object)TENANT_INFO_MAP.size());
        }
    }

    public static void destroy(String tenantId) {
        TenantInfo info = TENANT_INFO_MAP.remove(tenantId);
        if (log.isDebugEnabled()) {
            log.info("remove tenant information from local storage, id={}, size={}", (Object)info.getId(), (Object)TENANT_INFO_MAP.size());
        }
    }

    public static synchronized TenantInfo get(String tenantId) {
        return TenantInfoLocalStorage.get(tenantId, true);
    }

    public static TenantInfo get(String tenantId, boolean toLoad) {
        if (toLoad) {
            return TENANT_INFO_MAP.computeIfAbsent(tenantId, info -> TenantInfoLocalStorage.load(tenantId));
        }
        return TENANT_INFO_MAP.get(tenantId);
    }

    public static Set<TenantInfo> getAll() {
        return new HashSet<TenantInfo>(TENANT_INFO_MAP.values());
    }

    private static TenantInfo load(String tenantId) {
        Map getterMap = applicationContext.getBeansOfType(TenantInfoGetter.class);
        if (!CollectionUtils.isEmpty((Map)getterMap)) {
            TreeSet<TenantInfoGetter> getters = new TreeSet<TenantInfoGetter>(Comparator.comparingInt(TenantInfoGetter::order));
            getters.addAll(getterMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet()));
            for (TenantInfoGetter getter : getters) {
                try {
                    TenantInfo info = getter.get(tenantId);
                    if (info == null) continue;
                    return info;
                }
                catch (Throwable t) {
                    log.warn("load tenant information failed, tenantId=" + tenantId, t);
                }
            }
        }
        return null;
    }

    public void init() throws Exception {
        if (isInit.compareAndSet(false, true)) {
            this.loadAll();
            this.startStorageMonitor();
        }
    }

    private void loadAll() {
        this.loadAll(true);
    }

    private void loadAll(boolean logged) {
        Map getterMap = applicationContext.getBeansOfType(TenantInfoGetter.class);
        if (!CollectionUtils.isEmpty((Map)getterMap)) {
            TreeSet<TenantInfoGetter> getters = new TreeSet<TenantInfoGetter>(Comparator.comparingInt(TenantInfoGetter::order));
            getters.addAll(getterMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet()));
            getters.forEach(g -> {
                try {
                    Set<TenantInfo> loaded;
                    if (log.isDebugEnabled() && logged) {
                        log.info("==> getter: {}", (Object)g.name());
                    }
                    if (!CollectionUtils.isEmpty(loaded = g.getAll())) {
                        for (TenantInfo prepared : loaded) {
                            if (TENANT_INFO_MAP.containsKey(prepared.getId())) continue;
                            TENANT_INFO_MAP.put(prepared.getId(), prepared);
                            TenantEventListenerManager.onAdd(prepared);
                        }
                    }
                }
                catch (Throwable t) {
                    log.warn("find all tenant info failed, getter=" + g.name(), t);
                }
            });
        }
        if (log.isDebugEnabled() && logged) {
            log.info("load all tenant information complete, total={}", (Object)TENANT_INFO_MAP.size());
        }
    }

    private void startStorageMonitor() {
        this.monitor = THREAD_FACTORY.newThread((Runnable)TtlRunnable.get(() -> {
            while (!this.exit) {
                try {
                    Thread.sleep(10000L);
                }
                catch (InterruptedException e) {
                    this.exit = true;
                }
                this.loadAll(false);
            }
        }));
        this.monitor.setDaemon(true);
        this.monitor.start();
    }

    public void destroy() throws Exception {
        this.exit = true;
        TENANT_INFO_MAP.clear();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TenantInfoLocalStorage.applicationContext = applicationContext;
    }

    public static void setApplicationContext2(ApplicationContext applicationContext) {
        TenantInfoLocalStorage.applicationContext = applicationContext;
    }

    static {
        THREAD_FACTORY = new NamedThreadFactory("Tenant-Storage-Monitor");
    }
}

