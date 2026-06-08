package com.supcon.supfusion.framework.scaffold.auditlog.cache;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * 审计数据日志涉及模型缓存
 * @author caokele
 */
public final class AuditDataLogModelCache {
    private static final ThreadLocal<List<ModelObjectInfo>> MODEL_THREAD_LOCAL = new TransmittableThreadLocal<List<ModelObjectInfo>>() {
        @Override
        protected List<ModelObjectInfo> initialValue() {
            return new LinkedList<>();
        }
    };

    public static void add(ModelObjectInfo modelObjectInfo) {
        MODEL_THREAD_LOCAL.get().add(modelObjectInfo);
    }

    public static List<ModelObjectInfo> get() {
        return MODEL_THREAD_LOCAL.get();
    }

    public static void clear() {
        MODEL_THREAD_LOCAL.remove();
    }
}
