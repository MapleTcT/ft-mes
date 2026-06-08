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
 * @date: 2020年6月8日 下午4:50:50
 */
@Data
@ApiModel(value = "待办任务委托参数模型")
public class EntrustRequestVO extends VO{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 待办ID
     */
    @NotEmpty(message = "待办ID不能为空")
    @ApiModelProperty(value = "待办ID", name = "taskId", example = "35579", dataType = "String", required = true)
    private String taskId;
    /**
     * 委托原因
     */
    @ApiModelProperty(value = "委托原因", name = "reason", example = "医院看病")
    private String reason;
    /**
     * 受托者-人员编码
     */
    @NotEmpty(message = "被委托者不能为空")
    @ApiModelProperty(value = "受托者-用户ID", name = "mandatary", example = "10000001", required = true)
    private String mandatary;

}
