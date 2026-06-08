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
public class OodmFlowParamDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 流程实例ID
     */
    private String processId;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 开始时间
     */
    private String createTime;
    /**
     * 完成时间
     */
    private String completeTime;
    /**
     * 执行者
     */
    private String userId;
    /**
     * 执行者人员名称
     */
    private String staffName;
    /**
     * 发起者ID
     */
    private String initiatorId;
    /**
     * 发起者人员名称
     */
    private String initiatorName;
    
}
