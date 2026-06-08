/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import javax.validation.constraints.NotEmpty;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月9日 下午2:46:19
 */
@Data
public class IdRequestVO extends VO {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    @NotEmpty(message = "Id不能为空")
    @ApiModelProperty(value = "id", name="id", dataType = "String", example="580038889177088(String)", required = true)
    private String id;
}
