/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月10日 下午2:40:41
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@ApiModel(value = "ProcessStartResponseVO", description = "启动流程成功返回结果")
public class ProcessStartResponseVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程实例ID
     */
    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "30526")
    private String processId;
    
    /**
     * 待办任务ID
     */
    @ApiModelProperty(value = "待办任务ID", name = "taskId", dataType = "String", example = "30527")
    private String taskId;

}
