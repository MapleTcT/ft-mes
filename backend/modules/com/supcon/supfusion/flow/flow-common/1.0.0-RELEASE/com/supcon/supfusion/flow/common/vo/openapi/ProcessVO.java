/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月4日 上午9:13:00
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "ProcessVO", description = "流程模型")
public class ProcessVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    @ApiModelProperty(value = "APP ID", name = "appId", dataType = "String", example = "App_123456789")
    private String appId;
    /**
     * 流程ID
     */
    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "30526")
    private String processId;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", dataType = "String", example = "请假流程")
    private String processName;
    /**
     * 流程状态
     */
    @ApiModelProperty(value = "流程名称", name = "status", dataType = "Integer", example = "88-进行中 77-暂停 99-作废 66-已完成")
    private Integer status;
    /**
     * 发起时间
     */
    @ApiModelProperty(value = "流程发起时间", name = "startTime", dataType = "String", example = "2020-11-04 11:00:00")
    private String startTime;
    /**
     * 结束时间
     */
    @ApiModelProperty(value = "流程结束时间", name = "endTime", dataType = "String", example = "2020-11-04 11:00:00")
    private String endTime;
    /**
     * 当前环节
     */
    @ApiModelProperty(value = "当前环节", name = "tasks", dataType = "SimpleTaskVO", example = "[{\"userId\": \"33333333\", \"username\": \"张三\", \"taskId\": \"222222222\", \"taskName\": \"审批\"}]")
    private Collection<SimpleTaskVO> tasks;
}
