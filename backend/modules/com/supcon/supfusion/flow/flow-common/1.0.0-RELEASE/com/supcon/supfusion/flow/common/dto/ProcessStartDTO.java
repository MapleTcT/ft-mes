/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.List;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月27日 上午10:01:13
 */
@Data
public class ProcessStartDTO {
    
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 启动任务名称
     */
    private String startTaskName;
    /**
     * 关联页面url
     */
    private String url;
    /**
     * 当前页面是否只读 
     */
    private Boolean readOnly;
    /**
     * 是否开启备注 
     */
    private Boolean enableComment;
    /**
     * 是否开启保存功能
     */
    private boolean enableSave;
    /**
     * 分支信息
     */
    private List<AuditDTO> audits;
    /**
     * 指派信息
     */
    private List<AssigneeDTO> assigns;
}
