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
 * @date: 2020年6月9日 下午3:20:12
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel("委托数据模型")
public class EntrustResponseVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 记录ID
     */
    @ApiModelProperty(value = "数据id", name = "id", dataType = "String", example = "580038889177088(String)")
    private final String id;
    /**
     * 任务名称
     */
    @ApiModelProperty(value = "任务名称", name = "taskName", example = "请假审批")
    private final String taskName;
    /**
     * 单据编号
     */
    @ApiModelProperty(value = "单据编号", name = "formNo", example = "No123")
    private final String formNo;
    /**
     * 
     */
    @ApiModelProperty(value = "表单数据", name = "formData", example = "{}")
    private final String formData;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", example = "请假流程")
    private final String processName;
    
    @ApiModelProperty(value = "流程ID", name = "processId", example = "1591177203604")
    private final String processId;
    /**
     * 受托者名称
     */
    @ApiModelProperty(value = "受托者名称", name = "mandataryName", example = "张三")
    private final String mandataryName;
    /**
     * 委托时间
     */
    @ApiModelProperty(value = "受托者名称", name = "entrustTime", example = "1591177203604")
    private final String entrustTime;
    /**
     * 委托原因
     */
    @ApiModelProperty(value = "委托原因", name = "reason", example = "回家休息")
    private final String reason;
    
    @ApiModelProperty(value = "是否可显示流程日志", name = "showlog", example = "true")
    private final Boolean showlog;

    private EntrustResponseVO(Builder builder) {
        this.id = builder.id;
        this.taskName = builder.taskName;
        this.mandataryName = builder.mandataryName;
        this.reason = builder.reason;
        this.entrustTime = builder.entrustTime;
        this.processName = builder.processName;
        this.processId = builder.processId;
        this.formNo = builder.formNo;
        this.showlog = builder.showlog;
        this.formData = builder.formData;
    }

    public static class Builder {
        private String id;
        private String taskName;
        private String formNo;
        private String processName;
        private String processId;
        private String mandataryName;
        private String entrustTime;
        private String reason;
        private String formData;
        private Boolean showlog;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setFormData(String formData) {
            this.formData = formData;
            return this;
        }

        public Builder setProcessName(String processName) {
            this.processName = processName;
            return this;
        }

        public Builder setShowlog(Boolean showlog) {
            this.showlog = showlog;
            return this;
        }

        public Builder setFormNo(String formNo) {
            this.formNo = formNo;
            return this;
        }

        public Builder setProcessId(String processId) {
            this.processId = processId;
            return this;
        }

        public Builder setTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder setMandataryName(String mandataryName) {
            this.mandataryName = mandataryName;
            return this;
        }

        public Builder setEntrustTime(String entrustTime) {
            this.entrustTime = entrustTime;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public EntrustResponseVO build() {
            return new EntrustResponseVO(this);
        }

    }

}
