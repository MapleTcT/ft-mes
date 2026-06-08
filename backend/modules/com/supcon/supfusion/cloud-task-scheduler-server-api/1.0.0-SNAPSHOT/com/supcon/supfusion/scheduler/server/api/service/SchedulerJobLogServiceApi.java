package com.supcon.supfusion.scheduler.server.api.service;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.scheduler.server.api.vo.SchedulerJobLogVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@FeignClient(name = "supfusion-task-scheduler-service")
@ServiceApi(path = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "task-scheduler/v1")
@Validated
public interface SchedulerJobLogServiceApi {

    @GetMapping("/log/getJobLog")
    public Result getSchedulerJobLog(@ModelAttribute SchedulerJobLogVo schedulerJobLogVo);
}
