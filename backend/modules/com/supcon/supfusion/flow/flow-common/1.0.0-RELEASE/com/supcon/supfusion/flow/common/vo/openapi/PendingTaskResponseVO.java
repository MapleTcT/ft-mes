/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午5:30:46
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "PendingTaskResponseVO", description = "待办数据模型")
public class PendingTaskResponseVO extends VO {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    @ApiModelProperty(value = "app id", name = "appId", dataType = "String", example = "app_123456789")
    private final String appId;
    
    @ApiModelProperty(value = "company id", name = "companyId", dataType = "String", example = "23456789000000")
    private final String companyId;
    
    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "123456")
    protected final String processId;

    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456")
    protected final String taskId;

    @ApiModelProperty(value = "待办名称", name = "taskName", example = "编辑")
    protected final String taskName;

    @ApiModelProperty(value = "流程实例名称", name = "diagramName", example = "请假单-001")
    protected final String processName;

    @ApiModelProperty(value = "流程发起者", name = "initiator", example = "zhangsan")
    protected final String initiator;
    
    @ApiModelProperty(value = "待办接收时间", name = "startTime", example = "2020-09-01T11:11:11.000+0000")
    protected final String startTime;
    
    @ApiModelProperty(value = "单据数据", name = "formData", example = "{\"a\": 1}")
    protected final String formData;
    
    @ApiModelProperty(value = "页面地址", name = "url", example = "/supos/page/page_123456")
    protected final String url;
    
    @ApiModelProperty(value = "是否显示流程日志", name = "showlog", example = "false")
    protected final Boolean showlog;
    /**
     * 是否支持多公司 false-不支持  true-支持
     */
    @ApiModelProperty(value = "是否支持多公司", name = "multiCompany", example = "false")
    protected final Boolean multiCompany;
    
    /**
     * 待办状态
     * @see com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum
     */
    @ApiModelProperty(value = "待办状态", name = "status", example = "1-进行中  2-暂停")
    private final Integer status;
    
    @ApiModelProperty(value = "单据临时保存数据", name = "formTempData", example = "{\"a\": 2}")
    private final String formTempData;
    
    @ApiModelProperty(value = "是否可加签", name = "addInstance", example = "false-不能 true-可以")
    private final Boolean addInstance;
    
    @ApiModelProperty(value = "是否只读", name = "readonly", example = "true")
    private final Boolean readonly;
    
    @ApiModelProperty(value = "是否启用备注", name = "enableComment", example = "true")
    private final Boolean enableComment;
    
    @ApiModelProperty(value = "审批分支", name = "audits", example = "[{\"id\": \"K1236\", \"name\": \"分支名称-同意\", \"value\": \"分支值,决定流程走向\", "
            + "\"comment\": \"备注\", \"type\": \"分支线类型  0: 普通迁移线 1: 驳回线\"}]")
    private final List<AuditVO> audits;
    
    @ApiModelProperty(value = "重新指派", name = "assigns", example = "[{\"id\": \"K1236\", \"taskDefKey\": \"F1002\", \"name\": \"节点名称-审批\"}]")
    private final List<AssigneeVO> assigns;
    
    private PendingTaskResponseVO(Builder builder) {
        this.status = builder.status;
        this.companyId = builder.companyId;
        this.addInstance = builder.addInstance;
        this.formTempData = builder.formTempData;
        this.readonly = builder.readonly;
        this.enableComment = builder.enableComment;
        this.showlog = builder.showlog;
        this.formData = builder.formData;
        this.processId = builder.processId;
        this.processName = builder.processName;
        this.startTime = builder.startTime;
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.url = builder.url;
        this.initiator = builder.initiator;
        this.audits = builder.audits;
        this.assigns = builder.assigns;
        this.multiCompany = builder.multiCompany;
        this.appId = builder.appId;
    }

    public static class Builder {
        private String appId;
        private String companyId;
        private Boolean multiCompany;
        private String taskId;
        private String taskName;
        private Integer status;
        private String processName;
        private String startTime;
        private String formData;
        private String formTempData;
        private String url;
        private String initiator;
        private String processId;
        private Boolean addInstance;
        private Boolean readonly;
        private Boolean showlog;
        private Boolean enableComment;
        private List<AuditVO> audits;
        private List<AssigneeVO> assigns;
        
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder setCompanyId(String companyId) {
            this.companyId = companyId;
            return this;
        }

        public Builder setShowlog(Boolean showlog) {
            this.showlog = showlog;
            return this;
        }

        public Builder setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder setStatus(Integer status) {
            this.status = status;
            return this;
        }

        public Builder setMultiCompany(Boolean multiCompany) {
            this.multiCompany = multiCompany;
            return this;
        }

        public Builder setStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setInitiator(String initiator) {
            this.initiator = initiator;
            return this;
        }

        public Builder setProcessId(String processId) {
            this.processId = processId;
            return this;
        }

        public Builder setProcessName(String processName) {
            this.processName = processName;
            return this;
        }

        public Builder setFormData(String formData) {
            this.formData = formData;
            return this;
        }

        public Builder setFormTempData(String formTempData) {
            this.formTempData = formTempData;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setAddInstance(Boolean addInstance) {
            this.addInstance = addInstance;
            return this;
        }

        public Builder setReadonly(Boolean readonly) {
            this.readonly = readonly;
            return this;
        }

        public Builder setAssigns(List<AssigneeVO> assigns) {
            this.assigns = assigns;
            return this;
        }

        public Builder setEnableComment(Boolean enableComment) {
            this.enableComment = enableComment;
            return this;
        }

        public Builder setAudits(List<AuditVO> audits) {
            this.audits = audits;
            return this;
        }

        public PendingTaskResponseVO build() {
            return new PendingTaskResponseVO(this);
        }

    }
}
