package com.supcon.supfusion.scheduler.server.service.bo;

import lombok.Data;

@Data
public class SchedulerJobLogBo {
    private Long id;
    private String modelName;
    private String jobName;
    private String code;
    private String jobNameInternational;
    private Integer jobStatus;
    private String serviceApi;
    private String sorter;
    private  Integer current;
    private Integer pageSize;
    private Integer filter;
    private String userName;
    private String serviceParams;
    private Boolean fuzzySearch;
}
