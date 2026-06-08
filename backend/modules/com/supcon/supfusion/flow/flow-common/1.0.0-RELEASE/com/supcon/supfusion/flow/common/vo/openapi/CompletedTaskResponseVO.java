/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月25日 下午5:30:46
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "CompletedTaskResponseVO", description = "已办数据模型")
public class CompletedTaskResponseVO extends VO {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "123456")
    protected final String processId;
    
    @ApiModelProperty(value = "流程实例名称", name = "processName", example = "请假单-001")
    protected final String processName;
    
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456")
    protected final String taskId;

    @ApiModelProperty(value = "待办名称", name = "taskName", example = "编辑")
    protected final String taskName;

    @ApiModelProperty(value = "流程发起者", name = "initiator", example = "zhangsan")
    protected final String initiator;
    
    @ApiModelProperty(value = "单据编号", name = "formNo", example = "A001")
    protected final String formNo;
    
    @ApiModelProperty(value = "单据数据", name = "formData", example = "{\"a\": 1}")
    protected final String formData;
    
    @ApiModelProperty(value = "页面地址", name = "url", example = "/supos/page/page_123456")
    protected final String url;
    
    @ApiModelProperty(value = "是否可显示流程日志", name = "showlog", example = "true")
    private final Boolean showlog;
    
    @ApiModelProperty(value = "待办接收时间", name = "startTime", example = "2020-10-10 11:11:20")
    protected final String startTime;
    
    @ApiModelProperty(value = "完成时间", name = "endTime", example = "2020-10-10 11:11:20")
    private final String endTime;

    @ApiModelProperty(value = "是否可撤回", name = "revoke", example = "false/true")
    private final Boolean revoke;
    
    @ApiModelProperty(value = "是否驳回", name = "reject", example = "false/true")
    private final Boolean reject;
  
    
    private CompletedTaskResponseVO(Builder builder) {
        this.formNo = builder.formNo;
        this.initiator = builder.initiator;
        this.startTime = builder.startTime;
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.revoke = builder.revoke;
        this.processId = builder.processId;
        this.processName = builder.processName;
        this.endTime = builder.endTime;
        this.formData = builder.formData;
        this.url = builder.url;
        this.reject = builder.reject;
        this.showlog = builder.showlog;
    }

    public static class Builder {
        private String taskId;
        private String taskName;
        private String processName;
        private String startTime;
        private String endTime;
        private String formNo;
        private String formData;
        private String url;
        private String initiator;
        private String processId;
        private Boolean revoke;
        private Boolean showlog;
        private Boolean reject;
        
        public Builder setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder setShowlog(Boolean showlog) {
            this.showlog = showlog;
            return this;
        }

        public Builder setStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setFormNo(String formNo) {
            this.formNo = formNo;
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

        public Builder setEndTime(String endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder setRevoke(Boolean revoke) {
            this.revoke = revoke;
            return this;
        }

        public Builder setFormData(String formData) {
            this.formData = formData;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }


        public Builder setReject(Boolean reject) {
            this.reject = reject;
            return this;
        }

        public CompletedTaskResponseVO build() {
            return new CompletedTaskResponseVO(this);
        }

    }
}
