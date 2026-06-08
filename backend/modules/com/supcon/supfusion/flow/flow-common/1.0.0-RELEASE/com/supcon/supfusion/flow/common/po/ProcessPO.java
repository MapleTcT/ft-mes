/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月4日 下午1:10:48
 */
@Data
@TableName(value = "wfm_process", autoResultMap = true)
public class ProcessPO extends BaseEntity {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    private String appId;
    /**
     * 流程发起者
     */
    private Long userId; 
    /**
     * 流程发起者人员名称
     */
    private String staffName; 
    /**
     * 单据编号
     */
    private String tableNo;
    /**
     * 流程编号
     */
    private String processKey;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 版本
     */
    private Integer processVersion;
    /**
     * 流程状态 {@link com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum}
     */
    private Integer processStatus;
    /**
     * 结束时间
     */
    @TableField(
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String completeTime;
    /**
     * 公司ID
     */
    private Long cid;
    /**
     * 租户ID
     */
    private String tenantId;
    
}
