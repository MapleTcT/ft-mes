package com.supcon.supfusion.auditlog.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * 数据审计志查询模型
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataAuditLogQueryBO {

    /**
     * 所属模块名称，支持模糊搜索
     */
    private String moduleName;

    /**
     * 操作用户名数组
     */
    private List<String> userNames;

    /**
     * 操作开始时间
     */
    private String operateStartTime;

    /**
     * 操作结束时间
     */
    private String operateEndTime;

    /**
     * 表单名称，支持模糊查询
     */
    private String formName;

    /**
     * 被操作对象编码，支持模糊查询
     */
    private String modelObjCode;

    /**
     * 被操作对象名称，支持模糊查询
     */
    private String modelObjName;

    /**
     * 操作类型，支持多选
     */
    private List<String> operateTypes;

    /**
     * IP地址，支持模糊搜索
     */
    private String ipAddress;

    /**
     * 排序字段
     */
    private String sortKey;

    /**
     * 降序排序
     */
    private Boolean desc;

    /**
     * 操作描述
     */
    private String operateDesc;

    /**
     * 操作异常描述
     */
    private String operateErrorDesc;
}
