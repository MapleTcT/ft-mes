/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月2日 上午11:20:45
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "统计信息返回结果")
public class StatisticsVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", dataType = "String", example = "请假单")
    private String processName;
    /**
     * 活动名称
     */
    @ApiModelProperty(value = "活动名称", name = "taskName", dataType = "String", example = "编辑")
    private String taskName;
    /**
     * 总数
     */
    @ApiModelProperty(value = "统计总数", name = "count", dataType = "int", example = "1")
    private int count;
}
