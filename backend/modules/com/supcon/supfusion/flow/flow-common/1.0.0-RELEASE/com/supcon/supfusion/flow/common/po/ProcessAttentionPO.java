/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月30日 上午9:24:45
 */
@Data
@TableName(value = "wfm_process_attention", autoResultMap = true)
public class ProcessAttentionPO extends BaseEntity {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键
     */
    private Long id;
    /**
     * 
     */
    private String appId;
    /**
     * 流程ID
     */
    private String processId;
    /**
     * 关注着用户ID
     */
    private Long userId;
    /**
     * 单据编号
     */
    private String tableNo;
    /**
     * 发起人ID
     */
    private String initiatorId;
    /**
     * 发起人名称
     */
    private String staffName;
    /**
     * 租户ID
     */
    private String tenantId;
}
