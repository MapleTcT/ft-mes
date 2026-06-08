/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午5:30:46
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "CompleteTaskListResponseVO", description = "已办列表数据模型")
public class CompleteTaskListResponseVO implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
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
    
    @ApiModelProperty(value = "待办接收时间", name = "startTime", example = "2020-09-01 11:11:11")
    protected final String startTime;
    
    @ApiModelProperty(value = "待办完成时间", name = "endTime", example = "2020-09-01 11:11:11")
    protected final String endTime;

    @ApiModelProperty(value = "单据编号", name = "formNo", example = "A001")
    protected final String formNo;
    
    @ApiModelProperty(value = "页面地址", name = "url", example = "/supos/page/page_123456")
    protected final String url;
    
    @ApiModelProperty(value = "是否驳回", name = "reject", example = "false")
    protected final Boolean reject;
    
    private CompleteTaskListResponseVO(Builder builder) {
        this.formNo = builder.formNo;
        this.processId = builder.processId;
        this.processName = builder.processName;
        this.startTime = builder.startTime;
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.url = builder.url;
        this.initiator = builder.initiator;
        this.endTime = builder.endTime;
        this.reject = builder.reject;
    }

    public static class Builder {
        private String taskId;
        private String taskName;
        private Boolean reject;
        private String processName;
        private String startTime;
        private String endTime;
        private String formNo;
        private String url;
        private String initiator;
        private String processId;
        
        public Builder setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setReject(Boolean reject) {
            this.reject = reject;
            return this;
        }

        public Builder setEndTime(String endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder setTaskName(String taskName) {
            this.taskName = taskName;
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

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public CompleteTaskListResponseVO build() {
            return new CompleteTaskListResponseVO(this);
        }

    }
}
