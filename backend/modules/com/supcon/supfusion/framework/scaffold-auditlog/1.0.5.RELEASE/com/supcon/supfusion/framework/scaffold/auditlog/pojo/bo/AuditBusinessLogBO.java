package com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo;

import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

/**
 * 业务审计日志
 * @author caokele
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AuditBusinessLogBO implements AuditLogBO {

    /**
     * 链路跟踪ID
     */
    private Long traceId;
    /**
     * 模块名称（国际化key）
     */
    private String moduleName;
    /**
     * 模块编码
     */
    private String moduleCode;
    /**
     * 涉及模型对象信息
     */
    private List<ModelObjectInfo> modelObjects;
    /**
     * 主模型编码
     */
    private String mainModelCode;
    /**
     * 操作用户名称
     */
    private String operateUserName;
    /**
     * 操作时间
     */
    private Date operateTime;
    /**
     * 企业ID
     */
    private Long companyId;
    /**
     * 操作类型
     */
    private OperateType operateType;
    /**
     * IP地址
     */
    private String ipAddress;
    /**
     * 是否成功
     */
    private Boolean success;
    /**
     * 操作描述（国际化key）
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
}
