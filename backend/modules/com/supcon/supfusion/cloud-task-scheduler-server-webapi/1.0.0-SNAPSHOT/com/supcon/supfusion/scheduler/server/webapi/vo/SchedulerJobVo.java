package com.supcon.supfusion.scheduler.server.webapi.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.Date;

@Data
public class SchedulerJobVo {
    private Long id;
    private String modelName;
    @NotBlank(message = "所属模块编码不允许为空")
    private String moduleCode;
    @NotBlank(message = "任务名称不允许为空")
    @Length(max = 255, message = "任务名称最大长度为255字符")
    private String jobName;
    @NotBlank(message = "编码不允许为空")
    @Pattern(regexp = "^[a-zA-Z][A-Za-z0-9]{0,25}$", message = "编码仅支持以字母开头且长度不大于25的字母与数字")
    private String code;
    private String jobKey;
    private String jobNameInternational;
    private String jobCron;
    private Integer jobStatus;
    @Length(max = 255, message = "任务详情最大长度为255字符")
    private String jobDesc;
    @NotBlank(message = "接口url不允许为空")
    @Length(max = 255, message = "接口URL最大长度为255字符")
    private String serviceApi;
    private String sorter;
    private Integer current;
    private Integer pageSize;
    private Integer filter;
    private String userName;
    @Length(max = 255, message = "任务参数最大长度为255字符")
    private String serviceParams;
    /**
     * 上次调度时间
     */
    private Date lastTime;
    /**
     * 下次调度时间
     */
    private Date nextTime;
    /**
     * 调用次数
     */
    private Long callNo;
}
