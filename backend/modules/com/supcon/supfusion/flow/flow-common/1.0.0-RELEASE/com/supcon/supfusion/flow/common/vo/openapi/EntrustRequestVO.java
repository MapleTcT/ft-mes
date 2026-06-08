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
 * @date: 2020年6月8日 下午4:50:50
 */
@Data
@ApiModel(value = "EntrustRequestVO", description = "待办任务委托参数模型")
public class EntrustRequestVO extends BaseRequestVO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 待办ID
     */
    @ApiModelProperty(value = "待办ID", name = "taskId", example = "35579", dataType = "String", required = true)
    @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId")
    private String taskId;
    /**
     * 委托原因
     */
    @ApiModelProperty(value = "委托原因", name = "reason", example = "出差")
    private String reason;
    /**
     * 受托者名称
     */
    @ApiModelProperty(value = "受托者名称", name = "mandatary", example = "lisi", required = true)
    private String mandatary;

}
