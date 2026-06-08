package com.supcon.supfusion.framework.scaffold.auditlog.strategy;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.rpc.util.WebUtil;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditBusinessLog;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLog;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.AuditDataLogModelCache;
import com.supcon.supfusion.framework.scaffold.auditlog.event.AuditBusinessLogEvent;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 业务审计日志策略
 * @author caokele
 */
@Component
public class AuditBusinessLogStrategy implements AuditLogStrategy<AuditBusinessLogBO> {
    private static final Logger logger = LoggerFactory.getLogger(AuditBusinessLogStrategy.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AuditBusinessLogBO buildAuditLog() {
        AuditBusinessLogBO auditBusinessLogBO = new AuditBusinessLogBO();
        auditBusinessLogBO.setTraceId(RpcContext.getContext().getTraceId());
        UserContext userContext = UserContext.getUserContext();
        if (Objects.nonNull(userContext)) {
            auditBusinessLogBO.setOperateUserName(userContext.getUserName());
            auditBusinessLogBO.setCompanyId(userContext.getCompanyId());
        }
        auditBusinessLogBO.setIpAddress(WebUtil.getIP());
        auditBusinessLogBO.setOperateTime(new Date());
        return auditBusinessLogBO;
    }

    @Override
    public AuditBusinessLogBO buildAuditLog(Object arg) {
        if (arg instanceof AuditBusinessLog) {
            AuditBusinessLog auditBusinessLog = (AuditBusinessLog) arg;
            return buildByAuditBusinessLog(auditBusinessLog);
        } else if (arg instanceof AuditLog) {
            AuditLog auditLog = (AuditLog) arg;
            return buildByAuditLog(auditLog);
        }
        throw new IllegalArgumentException("The arg must be Audit Annotation!");
    }


    /**
     * 通过业务审计日志注解构建业务审计日志
     */
    private AuditBusinessLogBO buildByAuditBusinessLog(AuditBusinessLog auditBusinessLog) {
        AuditBusinessLogBO auditBusinessLogBO = buildAuditLog();
        auditBusinessLogBO.setModuleName(auditBusinessLog.moduleName());
        auditBusinessLogBO.setModuleCode(auditBusinessLog.moduleCode());
        auditBusinessLogBO.setOperateType(auditBusinessLog.operateType());
        auditBusinessLogBO.setDescription(auditBusinessLog.desc());
        auditBusinessLogBO.setMainModelCode(auditBusinessLog.mainModelCode());
        return auditBusinessLogBO;
    }

    /**
     * 通过通用审计日志注解构建业务审计日志
     */
    private AuditBusinessLogBO buildByAuditLog(AuditLog auditLog) {
        AuditBusinessLogBO auditBusinessLogBO = new AuditBusinessLogBO();
        auditBusinessLogBO.setModuleName(auditLog.moduleName());
        auditBusinessLogBO.setModuleCode(auditLog.moduleCode());
        auditBusinessLogBO.setOperateType(auditLog.operateType());
        auditBusinessLogBO.setDescription(auditLog.desc());
        auditBusinessLogBO.setMainModelCode(auditLog.mainModelCode());
        return auditBusinessLogBO;
    }

    @Override
    public void publishAuditLog(AuditBusinessLogBO auditLog) {
        auditLog.setModelObjects(AuditDataLogModelCache.get());
        applicationEventPublisher.publishEvent(new AuditBusinessLogEvent(Collections.singleton(auditLog)));
    }

    @Override
    public void publishAuditLogs(Collection<AuditBusinessLogBO> auditLogs) {
        List<ModelObjectInfo> modelObjects = AuditDataLogModelCache.get();
        for (AuditBusinessLogBO auditLog : auditLogs) {
            auditLog.setModelObjects(modelObjects);
        }
        applicationEventPublisher.publishEvent(new AuditBusinessLogEvent(auditLogs));
    }
}
