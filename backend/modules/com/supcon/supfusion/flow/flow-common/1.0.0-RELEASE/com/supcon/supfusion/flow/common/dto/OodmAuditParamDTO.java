/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年12月2日 下午4:05:16
 */
@Data
@JsonInclude(value = Include.NON_NULL)
public class OodmAuditParamDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 分支名称
     */
    private String name;
    /**
     * 是否驳回
     */
    private Boolean reject;
    /**
     * 执行哪条分支
     */
    private Integer order;
    
}
