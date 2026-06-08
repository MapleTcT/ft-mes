package com.supcon.supfusion.scheduler.server.openapi.vo;

import lombok.Data;

@Data
public class SchedulerJobLogVo {
    private Long id;
    private String modelName;
    private String jobName;
    private Integer jobStatus;
    private String serviceApi;
    private String sorter;
    private  Integer current;
    private Integer pageSize;
    private Integer filter;
    private String userName;
    private String serviceParams;
}
