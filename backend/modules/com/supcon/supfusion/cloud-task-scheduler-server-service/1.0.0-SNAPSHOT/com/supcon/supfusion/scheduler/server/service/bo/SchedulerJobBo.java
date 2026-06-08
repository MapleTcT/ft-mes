package com.supcon.supfusion.scheduler.server.service.bo;

import lombok.Data;

@Data
public class SchedulerJobBo {
    private Long id;
    private String modelName;
    private String moduleCode;
    private String jobName;
    private String code;
    private String jobKey;
    private String jobNameInternational;
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
