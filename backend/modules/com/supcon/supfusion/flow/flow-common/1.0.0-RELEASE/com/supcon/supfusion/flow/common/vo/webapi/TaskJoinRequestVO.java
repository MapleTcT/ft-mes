/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月15日 上午11:06:09
 */
@Data
@ApiModel(value = "加签参数模型")
public class TaskJoinRequestVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 待办实例ID
     */
    @NotEmpty(message = "task id 不能为空")
    @ApiModelProperty(value = "待办实例ID", name = "taskId", dataType = "String", example = "30526")
    private String taskId;
    /**
     * 受邀者列表 -- 编号
     */
    @NotNull(message = "受邀人不能为空")
    @ApiModelProperty(value = "受邀者列表", name = "invitee", example = "[\"123456789\", \"1234669888\"]")
    private List<String> invitee;

}
