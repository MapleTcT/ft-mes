package com.supcon.supfusion.scheduler.server.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author 刘旺
 * @version V1.0
 * @Package com.supcon.mare.scheduler.entity.po
 * @date 2020/7/20 11:18
 * @Copyright © 2020 中控（西安）
 */
@Data
@TableName(value = "scheduler_job_log_info", autoResultMap = true)
public class SchedulerJobLogPo extends BaseEntity implements Serializable {

    @TableId(value = "id")
    private Long id;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 任务编码
     */
    @TableField("code")
    private String code;

    /**
     * 模块名称
     */
    @TableField("model_Name")
    private String modelName;

    /**
     * 任务方法
     */
    @TableField("job_service_api")
    private String serviceApi;

    /**
     * 方法参数
     */
    @TableField("job_service_params")
    private String serviceParams;

    /**
     * 日志信息
     */
    @TableField("job_message")
    private String jobMessage;

    /**
     * 执行状态（0正常 1失败）
     */
    @TableField("job_status")
    private Integer jobStatus;


    /**
     * 异常信息
     */
    @TableField("exception_info")
    private String exceptionInfo;

    /**
     * 租户||分组
     */
    @TableField("user_name")
    private String userName;

}
