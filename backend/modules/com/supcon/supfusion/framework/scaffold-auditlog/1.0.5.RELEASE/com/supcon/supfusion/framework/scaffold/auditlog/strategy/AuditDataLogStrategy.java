package com.supcon.supfusion.framework.scaffold.auditlog.strategy;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLogModel;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.AuditDataLogModelCache;
import com.supcon.supfusion.framework.scaffold.auditlog.event.AuditDataLogEvent;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.util.ClassExUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 数据审计日志策略
 *
 * @author caokele
 */
@Component
public class AuditDataLogStrategy implements AuditLogStrategy<AuditDataLogBO> {
    private static final Logger logger = LoggerFactory.getLogger(AuditDataLogStrategy.class);
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AuditDataLogBO buildAuditLog() {
        AuditDataLogBO auditDataLogBO = new AuditDataLogBO();
        auditDataLogBO.setTraceId(RpcContext.getContext().getTraceId());
        auditDataLogBO.setOperateTime(new Date());
        return auditDataLogBO;
    }

    @Override
    public AuditDataLogBO buildAuditLog(Object model) {
        AuditDataLogBO auditDataLogBO = buildAuditLog();
        Class<?> modelClazz = model.getClass();
        AuditLogModel auditLogModel = modelClazz.getAnnotation(AuditLogModel.class);
        String entityName = StringUtils.isEmpty(auditLogModel.entityName()) ? modelClazz.getName() : auditLogModel.entityName();
        String entityCode = StringUtils.isEmpty(auditLogModel.entityCode()) ? modelClazz.getName() : auditLogModel.entityCode();
        String modelCode = auditLogModel.modelCode();
        String modelName = auditLogModel.modelName();
        auditDataLogBO.setModel(model);
        auditDataLogBO.setEntityName(entityName);
        auditDataLogBO.setEntityCode(entityCode);
        auditDataLogBO.setModelCode(modelCode);
        auditDataLogBO.setModelName(modelName);
        return auditDataLogBO;
    }

    @Override
    public void publishAuditLog(AuditDataLogBO auditLog) {
        applicationEventPublisher.publishEvent(new AuditDataLogEvent(Collections.singleton(auditLog)));
        ModelObjectInfo modelObjectInfo = convertModelInfo(auditLog);
        AuditDataLogModelCache.add(modelObjectInfo);
    }

    @Override
    public void publishAuditLogs(Collection<AuditDataLogBO> auditLogs) {
        applicationEventPublisher.publishEvent(new AuditDataLogEvent(auditLogs));
        for (AuditDataLogBO auditLog : auditLogs) {
            ModelObjectInfo modelObjectInfo = convertModelInfo(auditLog);
            AuditDataLogModelCache.add(modelObjectInfo);
        }
    }

    /**
     * 将审计数据日志转换为ModelInfo对象
     * @param auditLog 审计数据日志
     * @return 模型信息
     */
    private ModelObjectInfo convertModelInfo(AuditDataLogBO auditLog) {
        Object model = auditLog.getModel();
        if (model == null) {
            return null;
        }
        ModelObjectInfo modelObjectInfo = new ModelObjectInfo();
        modelObjectInfo.setModelCode(auditLog.getModelCode());
        modelObjectInfo.setModelName(auditLog.getModelName());
        modelObjectInfo.setEntityCode(auditLog.getEntityCode());
        modelObjectInfo.setEntityName(auditLog.getEntityName());
        try {
            Map<String, Object> modelMap = ClassExUtil.modelToMap(model);
            // 主键 code > id
            String code = (String)modelMap.get(ClassExUtil.FIELD_CODE);
            if (code != null) {
                modelObjectInfo.setModelObjCode(code);
                modelObjectInfo.setModelObjPk(code);
            } else {
                Object id = modelMap.get(ClassExUtil.FIELD_ID);
                if (id != null) {
                    modelObjectInfo.setModelObjPk(id.toString());
                }
            }
            Object name = modelMap.getOrDefault(ClassExUtil.FIELD_NAME, modelObjectInfo.getModelObjPk());
            if (name != null) {
                modelObjectInfo.setModelObjName(name.toString());
            }
        } catch (IllegalAccessException e) {
            logger.error("模型转map失败:", e);
        }
        return modelObjectInfo;
    }
}
