package com.supcon.supfusion.framework.scaffold.auditlog.pojo.po;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * 数据审计日志
 * @author caokele
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AuditDataLogPO implements AuditLogPO {
    /**
     * 链路跟踪ID
     */
    private Long traceId;
    /**
     * 实体名称（国际化key）
     */
    private String entityName;
    /**
     * 编码
     */
    private String entityCode;
    /**
     * 模型编码
     */
    private String modelCode;
    /**
     * 模型名称（国际化key）
     */
    private String modelName;
    /**
     * 操作类型
     */
    private String operateType;
    /**
     * 操作时间戳
     */
    private Long operateTime;
    /**
     * 模型对象
     */
    private Map<String, Object> model;
}
