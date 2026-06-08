package com.supcon.supfusion.framework.scaffold.auditlog.event;

import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * 数据审计日志事件
 * @author caokele
 */
public class AuditDataLogEvent extends ApplicationEvent {

    public AuditDataLogEvent(Collection<AuditDataLogBO> auditLogs) {
        super(auditLogs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AuditDataLogBO> getSource() {
        Object source = super.getSource();
        if (source == null) {
            return Collections.emptyList();
        }
        if (source instanceof Collection) {
            return (Collection<AuditDataLogBO>) source;
        }
        AuditDataLogBO auditDataLogBO = (AuditDataLogBO) source;
        return Collections.singletonList(auditDataLogBO);
    }
}
