package com.supcon.supfusion.framework.scaffold.auditlog.event;

import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * 业务审计日志事件
 * @author caokele
 */
public class AuditBusinessLogEvent extends ApplicationEvent {

    public AuditBusinessLogEvent(Collection<AuditBusinessLogBO> auditLogs) {
        super(auditLogs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AuditBusinessLogBO> getSource() {
        Object source = super.getSource();
        if (source == null) {
            return Collections.emptyList();
        }
        if (source instanceof Collection) {
            return (Collection<AuditBusinessLogBO>) source;
        }
        AuditBusinessLogBO auditBusinessLogBO = (AuditBusinessLogBO) source;
        return Collections.singletonList(auditBusinessLogBO);
    }
}
