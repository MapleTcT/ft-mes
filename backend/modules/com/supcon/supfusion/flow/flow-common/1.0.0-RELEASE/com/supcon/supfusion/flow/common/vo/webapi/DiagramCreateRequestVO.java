/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import javax.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 下午2:29:36
 */
@Data
@ApiModel("创建流程组态参数模型")
public class DiagramCreateRequestVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 记录ID
     */
    @ApiModelProperty(value = "app id", name = "appId", dataType = "String", example = "580038889177088(String)")
    private String appId;
    /**
     * 流程名称
     */
    @NotEmpty(message = "流程名称不能为空")
    @Length(max = 200, message = "流程名称长度不能超过200")
    @ApiModelProperty(value = "流程名称", name = "processName", example = "请假流程", required = true)
    private String processName;
    /**
     * 是否支持多公司
     */
    @ApiModelProperty(value = "是否支持多公司 false-不支持  true-支持", name = "multiCompany", example = "false", required = false)
    private Boolean multiCompany;

}
