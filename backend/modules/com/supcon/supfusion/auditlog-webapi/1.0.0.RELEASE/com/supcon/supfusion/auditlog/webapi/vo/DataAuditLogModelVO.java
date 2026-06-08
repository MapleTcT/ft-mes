package com.supcon.supfusion.auditlog.webapi.vo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * 数据审计日志
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataAuditLogModelVO {
    /**
     * 链路跟踪ID
     */
    private String traceId;

    /**
     * 模型编码
     */
    private String modelCode;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 被操作对象名称
     */
    private String modelObjName;

    /**
     * 被操作对象编码
     */
    private String modelObjCode;

    /**
     * 操作类型
     */
    private SystemCodeResultDTO operateType;

    /**
     * 操作描述
     */
    private String description;
}
