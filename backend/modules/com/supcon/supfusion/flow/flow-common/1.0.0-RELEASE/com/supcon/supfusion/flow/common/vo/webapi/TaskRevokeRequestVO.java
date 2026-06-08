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
 * @author: zhuangmh
 * @date: 2020年6月15日 上午11:06:09
 */
@Data
@ApiModel(value = "待办任务撤回参数模型")
public class TaskRevokeRequestVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 待办实例ID
     */
    @NotEmpty(message = "task id 不能为空")
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456")
    private String taskId;
    
}
