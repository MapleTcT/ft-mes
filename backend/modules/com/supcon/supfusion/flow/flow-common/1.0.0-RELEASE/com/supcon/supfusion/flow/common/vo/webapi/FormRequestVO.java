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
 * @date: 2020年9月2日 上午10:11:58
 */
@Data
@ApiModel(value = "表单请求参数模型")
public class FormRequestVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    @NotEmpty(message = "待办ID不能为空")
    @ApiModelProperty(value = "待办ID", name = "taskId", example = "35579", dataType = "String", required = true)
    private String taskId;
    
    @ApiModelProperty(value = "表单数据", name = "formData", example = "{}", dataType = "String", required = false)
    private String formData;

}
