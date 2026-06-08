/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月26日 下午3:03:34
 */
@Data
@ApiModel("催办请求参数模型")
public class UrgeRequestVO extends ProcessBaseRequestVO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 通知类型
     */
    @ApiModelProperty(value = "通知方式", name = "noticeType", dataType = "List", example = "[\"email\"]", required = true)
    @NotNull(message = "通知方式不能为空")
    private List<String> noticeType;
    
    @ApiModelProperty(value = "催办详情", name = "urgeList", dataType = "List", example = "[\"taskId\": \"8889992364568\", \"userIds\": [\"900102586479\"]]", required = true)
    @NotNull(message = "详情不能为空")
    private List<UrgeDetailVO> urgeList;
    

}
