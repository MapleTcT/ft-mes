package com.supcon.supfusion.framework.scaffold.auditlog.strategy;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.rpc.util.WebUtil;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditBusinessLog;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLog;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.AuditDataLogModelCache;
import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;
import com.supcon.supfusion.framework.scaffold.auditlog.event.AuditBusinessLogEvent;
import com.supcon.supfusion.framework.scaffold.auditlog.event.AuditDataLogEvent;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
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
        AuditBusinessLogBO auditBusinessLogBO = buildAuditLog();
        auditBusinessLogBO.setModuleName(auditLog.moduleName());
        auditBusinessLogBO.setModuleCode(auditLog.moduleCode());
        auditBusinessLogBO.setOperateType(auditLog.operateType());
        auditBusinessLogBO.setDescription(auditLog.desc());
        auditBusinessLogBO.setMainModelCode(auditLog.mainModelCode());
        return auditBusinessLogBO;
    }

    @Override
    public void publishAuditLog(AuditBusinessLogBO auditLog) {
        AuditDataLogModelCache.dealAuditDataLogs();
        auditLog.setModelObjects(AuditDataLogModelCache.getModelObjectInfoList());
        applicationEventPublisher.publishEvent(new AuditBusinessLogEvent(Collections.singleton(auditLog)));
        List<AuditDataLogBO> auditDataLogList = AuditDataLogModelCache.getAuditDataLogList();
        if (!auditDataLogList.isEmpty()) {
            dealPMStrangeDemands(auditDataLogList, auditLog);
            applicationEventPublisher.publishEvent(new AuditDataLogEvent(auditDataLogList));
        }
    }

    /**
     * 处理产品需求
     */
    private void dealPMStrangeDemands(List<AuditDataLogBO> auditDataLogList, AuditBusinessLogBO auditLog) {
        // BUG-145499 业务数据操作类型是驳回，那么数据日志的操作类型也得是驳回
        if (OperateType.REJECT.equals(auditLog.getOperateType())) {
            for (AuditDataLogBO auditDataLogBO : auditDataLogList) {
                auditDataLogBO.setOperateType(OperateType.REJECT);
            }
        }
        // 业务数据操作类型是新增，那么数据日志的操作类型也得是新增
        if (OperateType.ADD.equals(auditLog.getOperateType())) {
            for (AuditDataLogBO auditDataLogBO : auditDataLogList) {
                auditDataLogBO.setOperateType(OperateType.ADD);
            }
        }
    }

    @Override
    public void publishAuditLogs(Collection<AuditBusinessLogBO> auditLogs) {
        AuditDataLogModelCache.dealAuditDataLogs();
        List<ModelObjectInfo> modelObjects = AuditDataLogModelCache.getModelObjectInfoList();
        for (AuditBusinessLogBO auditLog : auditLogs) {
            auditLog.setModelObjects(modelObjects);
        }
        applicationEventPublisher.publishEvent(new AuditBusinessLogEvent(auditLogs));
        if (!AuditDataLogModelCache.getAuditDataLogList().isEmpty()) {
            applicationEventPublisher.publishEvent(new AuditDataLogEvent(AuditDataLogModelCache.getAuditDataLogList()));
        }
    }

    @Override
    public void publishAuditLogLazy(AuditBusinessLogBO auditLog) {
        throw new UnsupportedOperationException("Method not implemented!");
    }
}
