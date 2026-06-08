/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月5日 下午1:56:31
 */
@Data
@TableName(value = "wfm_process_log", autoResultMap = true)
public class ProcessLogPO extends BaseEntity {

    private static final long serialVersionUID = 1L;
    
    /**
     * 主键
     */
    private Long id;
    /**
     * 流程ID
     */
    private String processId;
    /**
     * 待办任务ID
     */
    private Long taskId;
    /**
     * 待办任务名称
     */
    private String taskName;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 审批结果
     */
    private String auditResult;
    /**
     * 备注
     */
    private String leaveComment;
    /**
     * 操作描述 
     * 格式: I18N_MESSAGE_CODE#参数1#参数2
     */
    private String actionDesc;
    /**
     * 操作类型 {@link com.supcon.supfusion.flow.common.enumeration.ProcessLogTypeEnum}
     */
    private String actionType;
    /**
     * 租户ID
     */
    private String tenantId;
    
}
