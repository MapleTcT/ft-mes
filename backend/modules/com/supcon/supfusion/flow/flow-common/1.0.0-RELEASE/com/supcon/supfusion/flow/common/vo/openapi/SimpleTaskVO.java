/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月10日 下午2:48:31
 */
@Data
@JsonInclude(value = Include.NON_NULL)
public class SimpleTaskVO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 环节处理人姓名
     */
    private String username;
    /**
     * 环节处理人ID
     */
    private String personName;
    /**
     * 当前环节ID
     */
    private String taskId;
    /**
     * 当前环节名称
     */
    private String taskName;
    
}
