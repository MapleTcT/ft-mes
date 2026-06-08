/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.OptimisticLockerBaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午5:03:16
 */
@Data
@TableName(value = "wfm_task_complete", autoResultMap = true)
public class CompleteTaskPO extends OptimisticLockerBaseEntity {
    
    private static final long serialVersionUID = 1L;
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 人员名称
     */
    private String personName;
    /**
     * 公司ID
     */
    private Long cid;
    /**
     * 流程编号
     */
    private String processKey;
    /**
     * 流程版本
     */
    private Integer processVersion;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 待办名称
     */
    private String taskName;
    /**
     * 人工节点ID
     */
    private String activityName;
    /**
     * 流程实例ID
     */
    private String processId;
    /**
     * 待办实例ID
     */
    private String instanceId;
    /**
     * 单据编号
     */
    private String tableNo;
    /**
     * 待办类型  0-普通待办 2-委托待办
     */
    private Integer taskType;
    /**
     * 表单数据
     */
    @TableField(exist=false)
    private String formData;
    /**
     * 关联页面url
     */
    private String openUrl;
    /**
     * 发起者
     */
    private String initiatorId;
    /**
     * 发起者人员名称
     */
    private String staffName;
    /**
     * 收到待办时间
     */
    @TableField(
            value = "start_time",
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String startTime;
    /**
     * 结束时间
     */
    @TableField(
            value = "end_time",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String endTime;
    /**
     * 待办来源
     * @see com.supcon.supfusion.flow.common.enumeration.TaskSourceEnum
     */
    private String taskSource;
    /**
     * 上一个环节待办提交者
     */
    private String latestUser;
    /**
     * 
     */
    private String appId;
    /**
     * 外部集成ID
     */
    private String integrationId;
    /**
     * 是否驳回
     */
    private Integer reject;
    /**
     * 最近一次委托者
     */
    private Long proxySource;
    /**
     * 委托的原始用户
     */
    private Long sourceStaff;
    /**
     * 租户ID
     */
    private String tenantId;
    
    private Integer showlog;
}
