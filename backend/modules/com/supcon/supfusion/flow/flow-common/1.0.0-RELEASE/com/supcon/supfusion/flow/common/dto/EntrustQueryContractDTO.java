/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Constants;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月9日 下午2:58:34
 */
@Data
public class EntrustQueryContractDTO {
    private final String appId;
    /**
     * 任务名称
     */
    private final List<String> taskNames;
    /**
     * 流程名称
     */
    private final List<String> processNames;
    /**
     * 受托者
     */
    private final List<String> mandatarys;
    /**
     * 委托开始时间
     */
    private final Date from;
    /**
     * 委托结束时间
     */
    private final Date to; 
    /**
     * 委托者
     */
    private final String principal;
    
    private EntrustQueryContractDTO(Builder builder) {
        this.appId = builder.appId;
        this.taskNames = builder.taskNames;
        this.processNames = builder.processNames;
        this.mandatarys = builder.mandatarys;
        this.from = builder.from;
        this.to = builder.to;
        this.principal = builder.principal;
    }
    
    public static class Builder {
        private String appId;
        private List<String> taskNames;
        private List<String> processNames;
        private List<String> mandatarys;
        private Date from;
        private Date to;
        private String principal;
        
        public Builder setProcessNames(String processNames) {
            if (processNames != null) {
                this.processNames = Arrays.asList(processNames.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setTaskNames(String taskNames) {
            if (taskNames != null) {
                this.taskNames = Arrays.asList(taskNames.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setMandatarys(String mandatarys) {
            if (mandatarys != null) {
                this.mandatarys =Arrays.asList(mandatarys.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setFrom(Date from) {
            this.from = from;
            return this;
        }
        public Builder setTo(Date to) {
            this.to = to;
            return this;
        }
        public Builder setPrincipal(String principal) {
            this.principal = principal;
            return this;
        }
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }
        public EntrustQueryContractDTO build() {
            return new EntrustQueryContractDTO(this);
        }
        
    }
    
}
