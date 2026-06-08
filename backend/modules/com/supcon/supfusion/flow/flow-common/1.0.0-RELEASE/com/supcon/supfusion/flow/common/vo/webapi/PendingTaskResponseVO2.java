/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

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
public class PendingTaskResponseVO2 extends VO {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "待办接收者", name = "assignee", dataType = "String", example = "zhangsan")
    private final String assignee;
    
    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "123456")
    private final String processId;

    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456")
    private final String taskId;

    @ApiModelProperty(value = "待办名称", name = "taskName", example = "编辑")
    private final String taskName;

    @ApiModelProperty(value = "流程实例名称", name = "diagramName", example = "请假单-001")
    private final String processName;

    @ApiModelProperty(value = "请求来源", name = "source", example = "supOS, bap")
    private final String source;
    
    @ApiModelProperty(value = "流程发起者", name = "initiator", example = "zhangsan")
    private final String initiator;
    
    @ApiModelProperty(value = "待办接收时间", name = "startTime", example = "2020-09-01 11:11:11")
    private final String startTime;

    @ApiModelProperty(value = "单据编号", name = "formNo", example = "A001")
    private final String formNo;
    
    @ApiModelProperty(value = "版本", name = "version", example = "12")
    private final Integer version;
    /**
     * 待办状态
     * @see com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum
     */
    @ApiModelProperty(value = "待办状态", name = "status", example = "1-进行中  2-暂停")
    private final Integer status;
    
    
    private PendingTaskResponseVO2(Builder builder) {
        this.status = builder.status;
        this.formNo = builder.formNo;
        this.processId = builder.processId;
        this.processName = builder.processName;
        this.source = builder.source;
        this.startTime = builder.startTime;
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.initiator = builder.initiator;
        this.assignee = builder.assignee;
        this.version = builder.version;
    }

    public static class Builder {
        private String assignee;
        private String taskId;
        private String taskName;
        private Integer status;
        private String processName;
        private String source;
        private String startTime;
        private String formNo;
        private String initiator;
        private String processId;
        private Integer version;
        
        public Builder setAssignee(String assignee) {
            this.assignee = assignee;
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

        public Builder setSource(String source) {
            this.source = source;
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

        public Builder setVersion(Integer version) {
            this.version = version;
            return this;
        }

        public PendingTaskResponseVO2 build() {
            return new PendingTaskResponseVO2(this);
        }

    }
}
