/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.supcon.supfusion.flow.common.util.Constants;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月4日 上午9:01:05
 */
@Data
public class CompleteTaskQueryContractDTO {
    
    private final String appId;
    
    /**
     * 待办名称
     */
    private final List<String> taskNames;
    /**
     * 单据编号
     */
    private final List<String> formNos;
    /**
     * 流程名称
     */
    private final List<String> processNames;
    /**
     * 发起者名字
     */
    private final List<String> initiators;
    /**
     * 待办接收开始时间
     */
    private final Date startFrom;
    /**
     * 待办接收结束时间
     */
    private final Date startTo;
    /**
     * 待办完成开始时间
     */
    private final Date completeFrom;
    /**
     * 待办完成结束时间
     */
    private final Date completeTo;
    
    private CompleteTaskQueryContractDTO(Builder builder) {
        this.taskNames = builder.taskNames;
        this.processNames = builder.processNames;
        this.formNos = builder.formNos;
        this.initiators = builder.initiators;
        this.startTo = builder.startTo;
        this.startFrom = builder.startFrom;
        this.completeFrom = builder.completeFrom;
        this.completeTo = builder.completeTo;
        this.appId = builder.appId;
    }
    
    /**
     *
     */
    public static class Builder {
        private String appId;
        private List<String> taskNames;
        private List<String> formNos;
        private List<String> processNames;
        private List<String> initiators;
        private Date startFrom;
        private Date startTo;
        private Date completeFrom;
        private Date completeTo;
        
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }
        public Builder setStartFrom(Date startFrom) {
            this.startFrom = startFrom;
            return this;
        }
        public Builder setStartTo(Date startTo) {
            this.startTo = startTo;
            return this;
        }
        public Builder setCompleteFrom(Date completeFrom) {
            this.completeFrom = completeFrom;
            return this;
        }
        public Builder setCompleteTo(Date completeTo) {
            this.completeTo = completeTo;
            return this;
        }
        public Builder setTaskNames(String taskNames) {
            if (taskNames != null) {
                this.taskNames = Arrays.asList(taskNames.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setFormNos(String formNos) {
            if (formNos != null) {
                this.formNos = Arrays.asList(formNos.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setProcessNames(String processNames) {
            if (processNames != null) {
                this.processNames = Arrays.asList(processNames.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setInitiators(String initiators) {
            if (initiators != null) {
                this.initiators = Arrays.asList(initiators.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public CompleteTaskQueryContractDTO build() {
            return new CompleteTaskQueryContractDTO(this);
        }
        
    }
}
