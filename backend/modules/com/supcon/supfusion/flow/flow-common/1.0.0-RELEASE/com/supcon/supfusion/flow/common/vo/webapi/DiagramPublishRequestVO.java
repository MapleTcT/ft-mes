/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import javax.validation.constraints.NotEmpty;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月26日 下午4:29:36
 */
@Data
@ApiModel("流程发布参数模型")
public class DiagramPublishRequestVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 记录ID
     */
    @NotEmpty(message = "主键不能为空")
    @ApiModelProperty(value = "记录ID", name = "id", dataType = "String", example = "580038889177088(String)", required = true)
    private String id;
    /**
     * 组态bpmn数据
     */
    @NotEmpty(message = "组态数据不能为空")
    @ApiModelProperty(value = "流程组态bpmn数据", name = "bpmnXml", required = true, example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions><process></process></definitions>")
    private String bpmnXml;

    /**
     * 组态数据
     */
    @ApiModelProperty(value = "流程组态数据", name = "json", example = "JSON DATA")
    private String json;

    /**
     * 是否要自动保存 0: 不需要保存 1: 需要保存
     */
    @ApiModelProperty(value = "是否自动保存, 默认 false", name = "autoSave", example = "true")
    private Boolean autoSave;
}
