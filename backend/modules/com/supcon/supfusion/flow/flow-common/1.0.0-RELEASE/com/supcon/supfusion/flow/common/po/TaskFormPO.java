/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月28日 下午2:42:14
 */
@Data
@TableName("wfm_task_form")
public class TaskFormPO extends BaseEntity {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 唯一ID
     */
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 流程实例ID
     */
    private String processId;
    /**
     * 任务实例ID
     */
    private String instanceId;
    /**
     * 单据数据
     */
    private String formData;
    /**
     * 单据临时保存的数据
     */
    private String formTempData;
}
