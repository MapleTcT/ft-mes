package com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo;

/**
 * 审计日志信息
 * @author caokele
 */
public interface AuditLogBO {
    /**
     * 获取链路跟踪ID
     */
    Long getTraceId();

    /**
     * 设置链路跟踪ID
     */
   void setTraceId(Long traceId);
}
