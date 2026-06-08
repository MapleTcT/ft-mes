/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.supcon.supfusion.flow.common.util.Constants;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月27日 下午2:43:26
 */
@Data
public class DiagramQueryContractDTO {
    
    /**
     * 流程名称
     */
    private final List<String> processName;
    /**
     * 是否只查询启用版本
     */
    private final Boolean enable;
    /**
     * 是否查询历史版本
     */
    private final Boolean history;
    /**
     * 是否跨公司
     */
    private final Integer multiCompany;
    /**
     * app id
     */
    private final String appId;
    /**
     * 创建者
     */
    private final List<String> creator;
    /**
     * 发布者
     */
    private final List<String> publisher;
    
    public DiagramQueryContractDTO(Builder builder) {
        this.processName = builder.processName;
        this.history = builder.history;
        this.enable = builder.enable;
        this.appId = builder.appId;
        this.creator = builder.creator;
        this.publisher = builder.publisher;
        this.multiCompany = builder.multiCompany;
    }
    
    public static class Builder {
        private List<String> processName;
        private Boolean enable;
        private Boolean history;
        private String appId;
        private List<String> creator;
        private List<String> publisher;
        private Integer multiCompany;
        
        public Builder setProcessName(String processName) {
            if (StringUtils.isNotEmpty(processName)) {
                this.processName = Arrays.asList(processName.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setEnable(String enable) {
            this.enable = Boolean.valueOf(enable);
            return this;
        }
        public Builder setHistory(String history) {
            this.history = Boolean.valueOf(history);
            return this;
        }
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }
        public Builder setCreator(String creator) {
            if (StringUtils.isNotEmpty(creator)) {
                this.creator = Arrays.asList(creator.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setPublisher(String publisher) {
            if (StringUtils.isNotEmpty(publisher)) {
                this.publisher = Arrays.asList(publisher.split(Constants.SPLIT_COMMA));
            }
            return this;
        }
        public Builder setMultiCompany(String multiCompany) {
            if (StringUtils.isNotEmpty(multiCompany)) {
                String[] split = multiCompany.split(Constants.SPLIT_COMMA);
                // 取值是/否, 多选==不选
                if (split.length == 1) {
                    this.multiCompany = Boolean.valueOf(split[0]) ? Constants.ENABLED : Constants.DISABLED;
                }
            }
            return this;
        }
        public DiagramQueryContractDTO build() {
            return new DiagramQueryContractDTO(this);
        }
    }
}
