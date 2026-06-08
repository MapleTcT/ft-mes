/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.ArrayList;
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
public class PendingQueryContractDTO {
    
    /**
     * 分类(task或process)
     */
    private final String category;
    
    private final String appId;
    
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
     * 发起者名字
     */
    private final List<String> initiators;
    /**
     * 待办状态 
     */
    private final List<Integer> status;
    /**
     * 执行者
     */
    private final List<String> assignees;
    /**
     * 流程版本
     */
    private final List<String> versions;
    /**
     * 待办接收开始时间
     */
    private final Date startFrom;
    /**
     * 待办接收结束时间
     */
    private final Date startTo;
    
    private PendingQueryContractDTO(Builder builder) {
        this.category = builder.category;
        this.ids = builder.ids;
        this.taskNames = builder.taskNames;
        this.processNames = builder.processNames;
        this.formNos = builder.formNos;
        this.initiators = builder.initiators;
        this.status = builder.status;
        this.assignees = builder.assignees;
        this.startTo = builder.startTo;
        this.startFrom = builder.startFrom;
        this.versions = builder.versions;
        this.appId = builder.appId;
    }
    
    /**
     *
     */
    public static class Builder {
        private String category;
        private String appId;
        private List<String> ids;
        private List<String> taskNames;
        private List<String> formNos;
        private List<String> processNames;
        private List<String> initiators;
        private List<Integer> status;
        private List<String> assignees;
        private List<String> versions;
        private Date startFrom;
        private Date startTo;
        
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }
        public Builder setAssignees(String assignees) {
            if (assignees != null) {
                this.assignees = Arrays.asList(assignees.split(Constants.SPLIT_COMMA));
            }
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
        public Builder setIds(String ids) {
            if (ids != null) {
                this.ids = Arrays.asList(ids.split(Constants.SPLIT_COMMA));
            }
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
        public Builder setVersions(String versions) {
            if (versions != null) {
                this.versions = Arrays.asList(versions.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setStatus(String status) {
            if (status != null) {
                String[] arrays = status.split(Constants.SPLIT_COMMA);
                List<Integer> statusList = new ArrayList<>();
                for (String array : arrays) {
                    statusList.add(Integer.parseInt(array));
                }
                this.status = statusList;
            }
            return this;
        }
        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }
        public PendingQueryContractDTO build() {
            return new PendingQueryContractDTO(this);
        }
        
    }
}
