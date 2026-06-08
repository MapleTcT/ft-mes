/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午1:54:04
 */
@Data
@ApiModel(value = "ProcessSaveRequestVO", description = "暂存流程参数模型")
public class ProcessSaveRequestVO extends BaseRequestVO {

    /**
     * 
     */
    private static final long serialVersionUID = -1L;
    
    /**
     * 流程编号
     */
    @ApiModelProperty(value = "流程编号", name = "processKey", example = "K2002018123456789", required = true)
    private String processKey;
    /**
     * 表单JSON数据
     */
    @ApiModelProperty(value = "表单数据", name = "formData", example = "{\"id\":1, \"name\": \"zhangsan\"}")
    private String formData;
    /**
     * 
     */
    @ApiModelProperty(value = "app id", name = "appId", example = "App_eab56959eef74c1ea2e1a5fbe7b38ddf")
    private String appId;
    
}
