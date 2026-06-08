package com.supcon.supfusion.framework.scaffold.auditlog.cache;

/**
 * 模型缓存操作类
 * @author caokele
 */
public class ModelCacheServiceImpl implements IModelCacheService {

    @Override
    public boolean isAuditLogEnabled(String modelCode) {
        return ModelAuditLogCache.SYSTEM_MODELS.contains(modelCode) || ModelAuditLogCache.EXTRA_MODELS.contains(modelCode);
    }

}
