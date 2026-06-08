package com.supcon.supfusion.framework.scaffold.auditlog.cache;

/**
 * 模型缓存
 *
 * @author caokele
 */
public interface IModelCacheService {

    /**
     * 判断模型是否启用审计日志
     *
     * @param modelCode 模型编码
     */
    boolean isAuditLogEnabled(String modelCode);

}
