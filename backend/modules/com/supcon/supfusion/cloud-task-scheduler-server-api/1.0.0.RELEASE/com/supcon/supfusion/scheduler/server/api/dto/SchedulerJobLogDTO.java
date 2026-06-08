package com.supcon.supfusion.scheduler.server.api.dto;

import lombok.Data;

@Data
public class SchedulerJobLogDTO {
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
