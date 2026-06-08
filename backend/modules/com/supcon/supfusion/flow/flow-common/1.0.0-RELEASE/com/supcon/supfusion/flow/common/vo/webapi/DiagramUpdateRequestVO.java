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
 * @Author: zhuangmh
 * @Date: 2020年5月19日 下午2:29:36
 */
@Data
@ApiModel("流程组态保存参数模型")
public class DiagramUpdateRequestVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 记录ID
     */
    @NotEmpty(message = "主键不能为空")
    @ApiModelProperty(value = "记录ID", name = "id", dataType = "String", example = "580038889177088(String)", required = true)
    private String id;
    /**
     * 组态数据
     */
    @NotEmpty(message = "组态数据不能为空")
    @ApiModelProperty(value = "流程组态数据", name = "json", example = "", required = true)
    private String json;

}
