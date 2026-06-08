/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月24日 下午2:03:36
 */
@Data
@AllArgsConstructor
@Api(value = "通知方式模型")
public class ProtocolVO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    private String key;
    /**
     * 
     */
    private String showName;

}
