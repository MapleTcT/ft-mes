/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2021年3月3日 下午7:26:33
 */
@Data
public class BaseRequestVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "用户名", name = "username", dataType = "String", example = "zhangsan", required = true)
    protected String username;

}
