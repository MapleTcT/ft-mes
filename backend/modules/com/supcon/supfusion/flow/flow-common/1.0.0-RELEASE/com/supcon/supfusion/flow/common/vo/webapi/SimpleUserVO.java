/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月26日 上午9:37:17
 */
@Data
@AllArgsConstructor
@ApiModel(value = "User", description = "用户模型")
public class SimpleUserVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID", name = "userId", dataType = "String", example = "305262222001")
    private String userId;
    /**
     * 人员名
     */
    @ApiModelProperty(value = "人员名", name = "staffName", dataType = "String", example = "小明")
    private String staffName;

}
