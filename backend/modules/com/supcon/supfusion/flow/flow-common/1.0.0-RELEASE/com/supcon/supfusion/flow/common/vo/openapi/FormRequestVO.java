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
 * @date: 2020年9月2日 上午10:11:58
 */
@Data
@ApiModel(value = "FormRequestVO", description = "表单请求参数模型")
public class FormRequestVO extends BaseRequestVO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "待办ID", name = "taskId", example = "35579", dataType = "String", required = true)
    @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId")
    private String taskId;
    
    @ApiModelProperty(value = "表单数据", name = "formData", example = "{}", dataType = "String", required = false)
    private String formData;

}
