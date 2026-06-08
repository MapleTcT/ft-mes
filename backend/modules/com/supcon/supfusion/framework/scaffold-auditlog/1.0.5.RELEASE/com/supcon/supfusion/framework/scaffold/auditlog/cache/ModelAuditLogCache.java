package com.supcon.supfusion.framework.scaffold.auditlog.cache;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 缓存启用了审计日志的模型
 *
 * @author caokele
 */
public class ModelAuditLogCache {
    private static IModelCacheService modelCacheService;
    /**
     * 系统启用审计日志的模型
     */
    public static final Set<String> SYSTEM_MODELS;
    /**
     * 拓展启用审计日志的模型
     */
    public static final Set<String> EXTRA_MODELS;

    static {
        SYSTEM_MODELS = new CopyOnWriteArraySet<>();
        EXTRA_MODELS = new CopyOnWriteArraySet<>();
    }

    public void setModelCache(IModelCacheService modelCache) {
        ModelAuditLogCache.modelCacheService = modelCache;
    }

    public static boolean isAuditLogEnabled(String modelCode) {
        return modelCacheService.isAuditLogEnabled(modelCode);
    }

    public static boolean isAuditLogEnabled(String[] modelCodes) {
        for (String modelCode : modelCodes) {
            if (isAuditLogEnabled(modelCode)) {
                return true;
            }
        }
        return false;
    }
}
