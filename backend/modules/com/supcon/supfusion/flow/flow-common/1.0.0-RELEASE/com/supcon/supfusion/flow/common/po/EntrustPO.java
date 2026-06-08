/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月8日 下午3:51:57
 */
@Data
@TableName(value = "wfm_entrust", autoResultMap = true)
public class EntrustPO extends BaseEntity {

    private static final long serialVersionUID = 1L;
    
    private Long id;
    /**
     * 待办实例ID
     */
    private String instanceId;
    
    private String appId;
    /**
     * 公司ID
     */
    private Long cid;
    /**
     * 委托方
     */
    private Long principal;
    /**
     * 被委托方
     */
    private Long mandatary;
    /**
     * 被委托方姓名
     */
    private String mandataryName;
    /**
     * 流程实例ID
     */
    private String processId;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 单据编号
     */
    private String tableNo;
    /**
     * 待办ID
     */
    private Long taskId;
    /**
     * 待办名称
     */
    private String taskName;
    /**
     * 委托原因
     */
    private String description;
    /**
     * 租户ID
     */
    private String tenantId;
   
}
