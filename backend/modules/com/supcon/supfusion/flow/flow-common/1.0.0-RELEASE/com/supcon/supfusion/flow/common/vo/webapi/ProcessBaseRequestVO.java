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
 * @date: 2020年6月16日 下午5:56:23
 */
@Data
@ApiModel(value = "流程參數基础模型")
public class ProcessBaseRequestVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程实例ID
     */
    @NotEmpty(message = "process id 不能为空")
    @ApiModelProperty(value = "流程实例ID", name = "processId", dataType = "String", example = "30526")
    private String processId;
}
