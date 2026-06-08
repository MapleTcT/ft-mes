package com.supcon.supfusion.scheduler.server.api.service;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.scheduler.server.api.constants.Constants;
import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobLogDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import static com.supcon.supfusion.scheduler.server.api.constants.Constants.API_PREFIX;

@FeignClient(name = "task-scheduler", contextId = "schedulerLog")
public interface ISchedulerJobLogApiService {

    @GetMapping(API_PREFIX + Constants.LOG_PATH + "/getJobLog")
    Result getSchedulerJobLog(@ModelAttribute SchedulerJobLogDTO schedulerJobLogDTO);
}
