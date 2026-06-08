/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年12月2日 上午9:51:20
 */
@Data
public class UrgeDetailVO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 待办ID
     */
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "1234", required = true)
    private String taskId;
    /**
     * 用户ID列表
     */
    @ApiModelProperty(value = "用户ID列表", name = "userIds", dataType = "List", example = "[\"12345678\"]", required = true)
    private List<String> userIds;
}
