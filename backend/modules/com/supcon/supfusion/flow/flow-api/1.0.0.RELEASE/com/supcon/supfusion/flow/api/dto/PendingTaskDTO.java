/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api.dto;

import java.io.Serializable;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;


/**
 * @author: zhuangmh
 * @date: 2020年9月29日 下午2:33:10
 */
public class PendingTaskDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 公司ID
     */
    private Long companyId;
    /**
     * 流程编号
     */
    private String diagramCode;
    /**
     * 流程名称
     */
    private String diagramName;
    /**
     * 流程版本
     */
    private Integer version;
    /**
     * 流程环节ID
     */
    private String nodeId;
    /**
     * 待办ID
     */
    private Long taskId;
    /**
     * 待办名称
     */
    private String taskName;
    /**
     * 流程实例ID
     */
    private String processId;
    
    /**
     * 单据编号
     */
    private String formNo;
    /**
     * 摘要
     */
    private String digest;
    /**
     * 关联页面url
     */
    private String url;
    /**
     * 执行者
     */
    private String userId;
    /**
     * 表单数据
     */
    private String formData;
    /**
     * 临时保存数据
     */
    private String formTempData;
    /**
     * 发起者-编号
     */
    private String initiator;
    /**
     * 发起者-名字
     */
    private String initiatorName;
    /**
     * 上一个环节提交者
     */
    private String sourceUser;
    /**
     * 上一个环节提交者
     */
    private String sourceUserName;
    /**
     * 是否支持多公司
     */
    private Boolean multiCompany;
    /**
     * 是否支持加签
     */
    private Boolean addInstance;
    /**
     * 是否只读
     */
    private Boolean readonly;
    /**
     * 是否启用备注
     */
    private Boolean enableComment;
    /**
     * 是否显示流程操作日志
     */
    private Boolean showlog;
    /**
     * 待办状态
     * @see com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum
     */
    private Integer taskStatus;
    /**
     * 收到待办时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 待办过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;
    /**
     * 待办来源
     * @see com.supcon.supfusion.flow.common.enumeration.RequestSourceEnum
     */
    private String taskSource;
    /**
     * 
     */
    private String appId;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 外部系统集成ID
     */
    private String integrationId;
}
