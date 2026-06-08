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
 * @date: 2020年11月4日 上午9:01:05
 */
@Data
public class ProcessQueryContractDTO {
    
    private final String appId;
    /**
     * 流程实例ID
     */
    private final List<String> ids;
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
     * 流程版本
     */
    private final List<String> versions;
    /**
     * 发起者名字
     */
    private final List<String> initiators;
    /**
     * 流程发起开始时间
     */
    private final Date startFrom;
    /**
     * 流程发起结束时间
     */
    private final Date startTo;
    
    private final Integer status;
    
    private ProcessQueryContractDTO(Builder builder) {
        this.taskNames = builder.taskNames;
        this.processNames = builder.processNames;
        this.formNos = builder.formNos;
        this.startTo = builder.startTo;
        this.startFrom = builder.startFrom;
        this.versions = builder.versions;
        this.status = builder.status;
        this.initiators = builder.initiators;
        this.ids = builder.ids;
        this.appId = builder.appId;
    }
    
    /**
     *
     */
    public static class Builder {
        private String appId;
        private List<String> ids;
        private List<String> taskNames;
        private List<String> formNos;
        private List<String> processNames;
        private List<String> versions;
        private List<String> initiators;
        private Date startFrom;
        private Date startTo;
        private Integer status;
        
        public Builder setStartFrom(Date startFrom) {
            this.startFrom = startFrom;
            return this;
        }
        public Builder setStartTo(Date startTo) {
            this.startTo = startTo;
            return this;
        }
        public Builder setTaskNames(String taskNames) {
            if (taskNames != null) {
                this.taskNames = Arrays.asList(taskNames.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setFormNos(String formNos) {
            if (formNos != null) {
                this.formNos = Arrays.asList(formNos.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setProcessNames(String processNames) {
            if (processNames != null) {
                this.processNames = Arrays.asList(processNames.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }
        public Builder setVersions(String versions) {
            if (versions != null) {
                this.versions = Arrays.asList(versions.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setInitiators(String initiators) {
            if (initiators != null) {
                this.initiators = Arrays.asList(initiators.split(Constants.COMMA));
            }
            return this;
        }
        public Builder setIds(String ids) {
            if (ids != null) {
                this.ids = Arrays.asList(ids.split(Constants.COMMA));
            }
            return this;
        }
        public ProcessQueryContractDTO build() {
            return new ProcessQueryContractDTO(this);
        }
        
    }
}
