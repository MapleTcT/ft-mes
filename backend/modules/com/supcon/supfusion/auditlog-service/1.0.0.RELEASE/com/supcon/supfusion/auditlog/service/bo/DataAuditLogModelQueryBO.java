package com.supcon.supfusion.auditlog.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * 数据审计日志
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataAuditLogModelQueryBO {

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
     * 操作类型，支持多选
     */
    private List<String> operateTypes;
}
