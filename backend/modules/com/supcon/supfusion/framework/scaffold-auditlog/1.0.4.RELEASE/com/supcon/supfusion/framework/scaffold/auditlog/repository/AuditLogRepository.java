package com.supcon.supfusion.framework.scaffold.auditlog.repository;


import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditLogPO;

import java.util.Collection;


/**
 * 审计日志存储
 * @author caokele
 */
public interface AuditLogRepository<T extends AuditLogPO> {

    Integer ASC = 1;

    Integer DESC = -1;

    void save(T auditLog);

    /**
     * 批量存储同一类的document集合
     */
    void batchSave(Collection<T> auditLogs);
}
