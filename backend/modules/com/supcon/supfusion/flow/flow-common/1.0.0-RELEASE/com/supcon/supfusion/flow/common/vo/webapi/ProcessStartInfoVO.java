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
 * @date: 2020年5月27日 上午10:01:13
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "ProcessStartInfoVO", description = "启动页数据模型")
public class ProcessStartInfoVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "company id", name = "companyId", dataType = "String", example = "23456789000000")
    private String companyId;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", example = "请假流程")
    private String processName;
    /**
     * 启动任务名称
     */
    @ApiModelProperty(value = "第一个人工环节名称", name = "startTaskName", example = "编辑")
    private String startTaskName;
    /**
     * 第一个人工环节关联页面url
     */
    @ApiModelProperty(value = "关联页面url", name = "url", example = "/page/123")
    private String url;
    /**
     * 当前页面是否只读 0: 可编辑 1: 只读
     */
    @ApiModelProperty(value = "当前页面是否只读 true/false", name = "readOnly", example = "false")
    private Boolean readOnly;
    /**
     * 是否开启备注 0: 关闭 1: 开启
     */
    @ApiModelProperty(value = "是否开启备注 true/false", name = "enableComment", example = "true")
    private Boolean enableComment;
    /**
     * 是否开启保存功能 0: 关闭 1: 开启
     */
    @ApiModelProperty(value = "是否开启保存功能 true/false", name = "enableSave", example = "false")
    private Boolean enableSave;
    
    @ApiModelProperty(value = "审批分支", name = "audits", example = "{\"id\": \"K1236\", \"name\": \"分支名称\", \"value\": \"分支值,决定流程走向\", "
            + "\"comment\": \"备注\", \"type\": \"分支线类型  0: 普通迁移线 1: 驳回线\"}")
    private List<AuditVO> audits;
    
    @ApiModelProperty(value = "是否支持多公司", name = "multiCompany", example = "false")
    private Boolean multiCompany;
    
    @ApiModelProperty(value = "重新指派", name = "assigns", example = "[{\"id\": \"1111222\", \"name\": \"提交\"}]")
    private List<AssigneeVO> assigns;
    
}
