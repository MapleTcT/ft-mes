package com.supcon.supfusion.scheduler.server.webapi.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class SchedulerJobLogVo {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务编码
     */
    private String code;

    /**
     * 模块名称
     */
    private String modelName;

    /**
     * 模块编码
     */
    private String moduleCode;
    /**
     * 任务方法
     */
    private String serviceApi;

    /**
     * 方法参数
     */
    private String serviceParams;

    /**
     * 日志信息
     */
    private String jobMessage;

    /**
     * 执行状态（0正常 1失败）
     */
    private Integer jobStatus;

    /**
     * 异常信息
     */
    private String exceptionInfo;

    /**
     * 租户||分组
     */
    private String userName;

    private String jobNameInternational;
    private String sorter;
    private Integer current;
    private Integer pageSize;
    private Integer filter;
    private Date createTime;
    private Boolean fuzzySearch = false;

    public static Date parseToDate(String dateString) {
        Date date = null;
        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
        }
        return date;
    }

}
