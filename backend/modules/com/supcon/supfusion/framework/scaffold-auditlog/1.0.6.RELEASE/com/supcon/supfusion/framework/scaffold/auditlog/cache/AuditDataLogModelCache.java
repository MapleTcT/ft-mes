package com.supcon.supfusion.framework.scaffold.auditlog.cache;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.util.ClassExUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 审计数据日志涉及模型缓存
 * @author caokele
 */
@Slf4j
public final class AuditDataLogModelCache {
    private static final ThreadLocal<List<ModelObjectInfo>> MODEL_OBJECT_INFO_THREAD_LOCAL = new TransmittableThreadLocal<List<ModelObjectInfo>>() {
        @Override
        protected List<ModelObjectInfo> initialValue() {
            return new LinkedList<>();
        }
    };

    private static final ThreadLocal<List<AuditDataLogBO>> AUDIT_DATA_LOG_THREAD_LOCAL = new TransmittableThreadLocal<List<AuditDataLogBO>>() {
        @Override
        protected List<AuditDataLogBO> initialValue() {
            return new LinkedList<>();
        }
    };

    public static void add(ModelObjectInfo modelObjectInfo) {
        MODEL_OBJECT_INFO_THREAD_LOCAL.get().add(modelObjectInfo);
    }

    public static void add(AuditDataLogBO auditDataLogBO) {
        AUDIT_DATA_LOG_THREAD_LOCAL.get().add(auditDataLogBO);
    }

    public static List<ModelObjectInfo> getModelObjectInfoList() {
        return MODEL_OBJECT_INFO_THREAD_LOCAL.get();
    }

    public static List<AuditDataLogBO> getAuditDataLogList() {
        return AUDIT_DATA_LOG_THREAD_LOCAL.get();
    }

    public static void clear() {
        MODEL_OBJECT_INFO_THREAD_LOCAL.remove();
        AUDIT_DATA_LOG_THREAD_LOCAL.remove();
    }

    /**
     * 审计日志数据处理
     * 对于同一种审计数据日志对象，只保留最后一次数据操作
     */
    public static void dealAuditDataLogs() {
        if (getAuditDataLogList().isEmpty()) {
            return;
        }
        List<AuditDataLogBO> auditDataLogList = getAuditDataLogList();
        // key：模型code  value.key: 对象pk  value.value: 审计数据日志
        Map<String, Map<String, AuditDataLogBO>> modelAuditDataLogMap = new HashMap<>(auditDataLogList.size());
        for (AuditDataLogBO auditDataLogBO : auditDataLogList) {
            String modelCode = auditDataLogBO.getModelCode();
            Map<String, Object> modelMap = null;
            try {
                modelMap = ClassExUtil.modelToMap(auditDataLogBO.getModel());
            } catch (IllegalAccessException e) {
                log.error("将模型对象转换成map失败", e);
                return;
            }
            Object id = modelMap.get(ClassExUtil.FIELD_ID);
            if (id == null) {
                log.error("未获取主键，审计数据日志无法存入");
                continue;
            }
            Map<String, AuditDataLogBO> pkAuditDataLogMap = modelAuditDataLogMap.computeIfAbsent(modelCode, k -> new HashMap<>());
            String pk = id.toString();
            pkAuditDataLogMap.put(pk, auditDataLogBO);
        }
        auditDataLogList.clear();
        modelAuditDataLogMap.forEach((code, pkAuditDataLogMap) -> {
            pkAuditDataLogMap.forEach((pk, auditDataLog) -> {
                ModelObjectInfo modelObjectInfo = ClassExUtil.convertModelInfo(auditDataLog);
                add(modelObjectInfo);
                add(auditDataLog);
            });
        });
    }
}
