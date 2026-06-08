/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import org.apache.ibatis.annotations.ResultMap;
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
@TableName(value = "wfm_task_pending", autoResultMap = true)
public class PendingTaskPO extends OptimisticLockerBaseEntity {

    private static final long serialVersionUID = 1L;
    /**
     * task id
     */
    private Long id;
    /**
     * 公司ID
     */
    private Long cid;
    /**
     *
     */
    private String appId;
    /**
     * 任务节点描述
     */
    private String taskDescription;
    /**
     * 活动类型
     */
    private String activityType;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 活动实例ID
     */
    private String executionId;
    /**
     * 待办状态 88-进行中 77-暂停
     */
    private Integer taskStatus;
    /**
     * 待办关联url
     */
    private String openUrl;
    /**
     * bap字段
     */
    private String instanceId;
    /**
     * 待办接收时间
     */
    @TableField(
            value = "start_time",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String startTime;
    /**
     * 流程编号
     */
    private String processKey;
    /**
     * 流程版本
     */
    private Integer processVersion;
    /**
     * 执行者
     */
    private Long userId;
    /**
     * 执行者人员名称
     */
    private String personName;
    /**
     * 表单数据
     */
    @TableField(exist = false)
    private String formData;
    /**
     * 临时保存数据
     */
    @TableField(exist = false)
    private String formTempData;

    /**
     * bap
     */
    @TableField(exist = false)
    private Integer count;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 流程定义描述
     */
    private String processDescription;
    /**
     * 发起者编号
     */
    private String initiatorId;
    /**
     * 发起者人员名称
     */
    private String staffName;
    /**
     * 上一个环节提交者人员名称
     */
    private String latestUser;
    /**
     * 流程实例ID
     */
    private String processId;
    /**
     * bap字段
     */
    private Long tableInfoId;
    /**
     * bap字段
     */
    private String entityCode;
    /**
     * 单据编号
     */
    private String tableNo;
    /**
     * 流程部署ID
     */
    private Long deploymentId;
    /**
     * 待办类型 0-普通待办 2-委托待办
     */
    private Integer taskType;
    /**
     * 是否关注
     */
    private Integer attention;
    /**
     * 0-不跨公司 1-跨公司
     */
    private Integer multiCompany;
    /**
     * 最近一次委托者
     */
    private Long proxySource;
    /**
     * 委托原因
     */
    private String description;
    /**
     * 循环会签
     * 0不是循环会签,1表示本公司，2表示夸公司，3表示本部门,4表示本部门及下级，5自定义
     */
    private Integer loops;
    /**
     * bap字段
     */
    private Long modelId;
    /**
     * 是否主办人
     */
    private Integer mainLoop;
    /**
     * 委托的原始用户
     */
    private Long sourceStaff;
    /**
     * 是否启动移动客户端审批
     */
    private Integer mobileApprove;
    /**
     * 是否已读, 0-未读 1-已读
     */
    private Integer hasRead;
    /**
     * 待办来源
     *
     * @see com.supcon.supfusion.flow.common.enumeration.TaskSourceEnum
     */
    private String taskSource;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 外部系统集成ID
     */
    private String integrationId;
    /**
     * 待办中文名称
     */
    private String taskDescriptionZhCn;
}
