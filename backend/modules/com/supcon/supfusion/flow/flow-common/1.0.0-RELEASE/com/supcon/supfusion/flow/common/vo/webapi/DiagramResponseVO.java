/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 下午2:29:36
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel("流程组态数据模型")
public class DiagramResponseVO extends VO {

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
     * 公司ID
     */
    @ApiModelProperty(value = "公司id", name = "companyId", dataType = "String", example = "580038889177088(String)")
    private final String companyId;
    /**
     * App ID
     */
    @ApiModelProperty(value = "app id", name = "appId", dataType = "String", example = "580038889177078(String)")
    private final String appId;
    /**
     * App ID
     */
    @ApiModelProperty(value = "app name", name = "appName", dataType = "String", example = "综合管理")
    private final String appName;
    /**
     * 流程编号
     */
    @ApiModelProperty(value = "流程编号", name = "processKey", example = "K2002018123456789")
    private final String processKey;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", example = "请假流程")
    private final String processName;
    /**
     * 组态数据
     */
    @ApiModelProperty(value = "流程组态JSON数据", name = "json", example = "{}")
    private final String json;
    /**
     * 流程状态
     * 
     * @see com.supcon.supfusion.flow.common.enumeration.DiagramStatusEnum
     */
    @ApiModelProperty(value = "流程状态", name = "status", example = "1")
    private final Integer status;
    /**
     * 流程版本
     */
    @ApiModelProperty(value = "流程版本", name = "version", example = "1")
    private final Integer version;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "流程创建者", name = "creator", example = "zhangsan")
    private final String creator;
    /**
     * 上一次操作时间
     */
    @ApiModelProperty(value = "上一次操作时间", name = "latestModifyTime", example = "2021-02-05T07:55:37.000+0000")
    private final String latestModifyTime;
    /**
     * 发布者
     */
    @ApiModelProperty(value = "发布者", name = "publisher", example = "lisi")
    private final String publisher;
    /**
     * 发布时间
     */
    @ApiModelProperty(value = "发布时间", name = "publishTime", example = "2021-02-05T07:55:37.000+0000")
    private final String publishTime;
    
    @ApiModelProperty(value = "是否支持多公司 false-不支持  true-支持", name = "multiCompany", example = "true")
    private final Boolean multiCompany;
    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用 true-启用", name = "enable", example = "true")
    private final Boolean enable;
    /**
     * 通知方式
     */
    @ApiModelProperty(value = "通知方式", name = "protocols", example = "[{\"key\": \"email\", \"showName\": \"邮件\"}]")
    private final List<ProtocolVO> protocols;

    public DiagramResponseVO(Builder builder) {
        this.id = builder.id;
        this.companyId = builder.companyId;
        this.processKey = builder.processKey;
        this.processName = builder.processName;
        this.status = builder.status;
        this.json = builder.json;
        this.version = builder.version;
        this.creator = builder.creator;
        this.latestModifyTime = builder.latestModifyTime;
        this.publisher = builder.publisher;
        this.publishTime = builder.publishTime;
        this.multiCompany = builder.multiCompany;
        this.appId = builder.appId;
        this.appName = builder.appName;
        this.enable = builder.enable;
        this.protocols = builder.protocols;
    }

    public static class Builder {
        private String id;
        private String companyId;
        private String processKey;
        private String processName;
        private String json;
        private Integer status;
        private Integer version;
        private String creator;
        private String latestModifyTime;
        private String publisher;
        private String publishTime;
        private Boolean multiCompany;
        private String appId;
        private String appName;
        private Boolean enable;
        private List<ProtocolVO> protocols;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setCompanyId(String companyId) {
            this.companyId = companyId;
            return this;
        }

        public Builder setProcessKey(String processKey) {
            this.processKey = processKey;
            return this;
        }

        public Builder setProcessName(String processName) {
            this.processName = processName;
            return this;
        }

        public Builder setJson(String json) {
            this.json = json;
            return this;
        }

        public Builder setStatus(Integer status) {
            this.status = status;
            return this;
        }

        public Builder setVersion(Integer version) {
            this.version = version;
            return this;
        }

        public Builder setCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder setLatestModifyTime(String latestModifyTime) {
            this.latestModifyTime = latestModifyTime;
            return this;
        }

        public Builder setPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder setPublishTime(String publishTime) {
            this.publishTime = publishTime;
            return this;
        }

        public Builder setMultiCompany(Boolean multiCompany) {
            this.multiCompany = multiCompany;
            return this;
        }

        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder setEnable(Boolean enable) {
            this.enable = enable;
            return this;
        }

        public Builder setProtocols(List<ProtocolVO> protocols) {
            this.protocols = protocols;
            return this;
        }

        public DiagramResponseVO build() {
            return new DiagramResponseVO(this);
        }

    }
}
