/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月17日 上午10:21:18
 */
@Data
@AllArgsConstructor
public class HelperResponseVO extends VO {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 文档说明 key-value 列表
     */
    @ApiModelProperty(value = "文档说明 key-value 列表", name = "docs", example = "[{\"key\": \"value\"}]")
    private List<KeyValuePair<String>> docs;
}
