package com.supcon.supfusion.scheduler.server.api.vo;

import lombok.Data;

@Data
public class SchedulerJobVo {
    private Long id;
    private String modelName;
    private String jobName;
    private String jobCron;
    private Integer jobStatus;
    private String jobDesc;
    private String serviceApi;
    private String sorter;
    private  Integer current;
    private Integer pageSize;
    private Integer filter;
    private String userName;
    private String serviceParams;
}
