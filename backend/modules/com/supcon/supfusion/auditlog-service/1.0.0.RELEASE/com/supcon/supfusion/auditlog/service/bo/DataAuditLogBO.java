package com.supcon.supfusion.auditlog.service.bo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
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
public class DataAuditLogBO {
    /**
     * 链路跟踪ID
     */
    private String traceId;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 操作用户名称
     */
    private String operateUserName;
    /**
     * 操作时间
     */
    private String operateTime;
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
     * IP地址
     */
    private String ipAddress;
    /**
     * 是否成功
     */
    private Boolean success;
    /**
     * 操作描述
     */
    private String description;
    /**
     * 操作异常描述
     */
    private String exceptionDescription;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 数据审计日志列表
     */
    private List<DataAuditLogBO> children;
}
