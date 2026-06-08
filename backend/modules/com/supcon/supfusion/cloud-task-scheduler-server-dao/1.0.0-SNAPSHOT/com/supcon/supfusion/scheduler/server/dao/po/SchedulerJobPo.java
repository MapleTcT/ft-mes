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


@Data
@TableName(value = "scheduler_job_info", autoResultMap = true)
public class  SchedulerJobPo extends BaseEntity implements Serializable {

    @TableId(value = "id")
    private Long id;

    /**
     * 模块名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 模块编码
     */
    @TableField("module_code")
    private String moduleCode;

    /**
     * 任务job名称，一般就是任务所在服务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 任务编码
     */
    @TableField("code")
    private String code;

    /**
     * 任务job名称国际化Key值
     */
    @TableField("job_key")
    private String jobKey;

    /**
     * 任务cron表达式
     */
    @TableField("job_cron")
    private String jobCron;

    /**
     * 0 - 执行  1 - 停止
     */
    @TableField("job_status")
    private Integer jobStatus = 0;

    /**
     * 任务说明
     */
    @TableField("job_desc")
    private String jobDesc;

    /**
     * 远程http调用接口
     */
    @TableField("job_service_api")
    private String serviceApi;

    /**
     * 调用参数
     */
    @TableField("job_service_params")
    private String serviceParams;

    /**
     * 调用次数
     */
    @TableField("job_call_No")
    private Long callNo = 0L;

    /**
     * 上次调度时间
     */
    @TableField("last_time")
    private Date lastTime;

    /**
     * 下次调度时间
     */
    @TableField("next_time")
    private Date nextTime;

    /**
     * 租户|jobGroup
      */
    @TableField("user_name")
    private String userName;

}
