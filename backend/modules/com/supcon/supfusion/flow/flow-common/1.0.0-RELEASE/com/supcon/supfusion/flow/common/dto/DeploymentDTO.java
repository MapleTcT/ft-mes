/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.io.InputStream;

import lombok.Data;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月26日 上午9:28:31
 */
@Data
public class DeploymentDTO {
    
    private final String deployId;
    /**
     * 流程组态模板ID
     */
    private final String processDefinitionId;
    /**
     * 流程组态模板xml
     */
    private final String processDefinitionXml;
    /**
     * 流程组态数据流
     */
    private final InputStream processInputStream;
    /**
     * 流程组态模板版本ID
     */
    private final int processDefinitionVersion;
    
    private DeploymentDTO(Builder builder) {
        this.deployId = builder.deployId;
        this.processDefinitionId = builder.processDefinitionId;
        this.processDefinitionXml = builder.processDefinitionXml;
        this.processInputStream = builder.processInputStream;
        this.processDefinitionVersion = builder.processDefinitionVersion;
    }
    
    public static class Builder {
        private String deployId;
        private String processDefinitionId;
        private String processDefinitionXml;
        private InputStream processInputStream;
        private int processDefinitionVersion;
        
        public Builder setDeployId(String deployId) {
            this.deployId = deployId;
            return this;
        }
        
        public Builder setProcessDefinitionId(String processDefinitionId) {
            this.processDefinitionId = processDefinitionId;
            return this;
        }
        public Builder setProcessDefinitionXml(String processDefinitionXml) {
            this.processDefinitionXml = processDefinitionXml;
            return this;
        }
        public Builder setProcessDefinitionVersion(int processDefinitionVersion) {
            this.processDefinitionVersion = processDefinitionVersion;
            return this;
        }
        public void setProcessInputStream(InputStream processInputStream) {
            this.processInputStream = processInputStream;
        }

        public DeploymentDTO build() {
            return new DeploymentDTO(this);
        }
    }
}
