package com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo;


import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 数据审计日志
 * @author caokele
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AuditDataLogBO implements AuditLogBO{
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
    private OperateType operateType;
    /**
     * 操作时间
     */
    private Date operateTime;
    /**
     * 模型对象
     */
    private Object model;
}
