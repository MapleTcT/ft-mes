/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.Collection;
import java.util.List;

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
@ApiModel(value = "流程列表模型")
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
     * 流程版本
     */
    @ApiModelProperty(value = "流程版本", name = "version", example = "1")
    private Integer version;
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
    
    @ApiModelProperty(value = "流程发起者", name = "initiator", example = "zhangsan")
    private String initiator;
    /**
     * 
     */
    @ApiModelProperty(value = "表单数据", name = "formData", example = "{}")
    private String formData;
    /**
     * 当前环节
     */
    @ApiModelProperty(value = "当前环节", name = "tasks", dataType = "PendingTaskResponseVO2", example = "[{\"taskId\":\"123\", \"taskName\": \"审批\"}]")
    private Collection<PendingTaskResponseVO2> tasks;
    
    @ApiModelProperty(value = "是否可显示流程日志", name = "showlog", example = "true")
    private Boolean showlog;
    
    @ApiModelProperty(value = "单据编号", name = "formNo", example = "A001")
    private String formNo;
}
