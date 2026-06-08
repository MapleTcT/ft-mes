/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月15日 上午11:06:09
 */
@Data
@ApiModel(value = "TaskRevokeRequestVO", description = "待办任务撤回参数模型")
public class TaskRevokeRequestVO extends BaseRequestVO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 待办实例ID
     */
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456")
    @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId")
    private String taskId;
    
}
