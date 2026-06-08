package com.supcon.supfusion.framework.scaffold.auditlog.strategy;


import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLogModel;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.ModelAuditLogCache;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditLogBO;

import java.util.Collection;
import java.util.Objects;

/**
 * 审计日志策略
 *
 * @author caokele
 */
public interface AuditLogStrategy<T extends AuditLogBO> {

    /**
     * 判断模型是否需要审计
     */
    default boolean isModelNeedAudit(Object model) {
        if (Objects.isNull(model)) {
            return false;
        }
        Class<?> modelClass = model.getClass();
        if (!modelClass.isAnnotationPresent(AuditLogModel.class)) {
            return false;
        }
        AuditLogModel auditLogModel = modelClass.getAnnotation(AuditLogModel.class);
        return ModelAuditLogCache.isAuditLogEnabled(auditLogModel.modelCode());
    }

    /**
     * 构建审计日志对象基本信息
     */
    T buildAuditLog();

    /**
     * 构建审计日志对象
     *
     * @param object 注解信息或数据模型
     */
    T buildAuditLog(Object object);

    /**
     * 发布审计日志
     *
     * @param auditLog 审计日志信息
     */
    void publishAuditLog(T auditLog);

    /**
     * 发布审计日志
     *
     * @param auditLogs 审计日志信息集合
     */
    void publishAuditLogs(Collection<T> auditLogs);
}
