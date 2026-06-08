package com.supcon.supfusion.scheduler.server.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.openapi.vo.SchedulerJobLogVo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobLogService;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobLogBo;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 刘旺
 * @version V1.0
 * @Package com.supcon.mare.scheduler.controller
 * @date 2020/7/20 15:11
 * @Copyright © 2020 中控（西安）
 */
@RestController
@Data
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "task-scheduler/v1")
public class SchedulerJobLogController extends BaseController {


    @Autowired
    private SchedulerJobLogService schedulerJobLogService;

    @GetMapping("/log/getJobLog")
    public Result getSchedulerJobLog(@ModelAttribute SchedulerJobLogVo schedulerJobLogVo) {
        SchedulerJobLogBo schedulerJobLogBo = new SchedulerJobLogBo();
        BeanUtils.copyProperties(schedulerJobLogVo, schedulerJobLogBo);
        String tenantId = RpcContext.getContext().getTenantId();
        schedulerJobLogBo.setUserName(tenantId);
        Page<SchedulerJobLogPo> schedulerJobLog = schedulerJobLogService.getSchedulerJobLog(schedulerJobLogBo);
        return Result.data(200,"success",schedulerJobLog);
    }


}
