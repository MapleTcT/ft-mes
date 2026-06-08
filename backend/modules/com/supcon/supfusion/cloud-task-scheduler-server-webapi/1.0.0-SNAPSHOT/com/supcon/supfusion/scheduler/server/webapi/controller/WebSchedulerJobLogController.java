package com.supcon.supfusion.scheduler.server.webapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobLogBo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobLogService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.scheduler.server.webapi.vo.SchedulerJobLogVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 刘旺
 * @version V1.0
 * @Package com.supcon.mare.scheduler.controller
 * @date 2020/7/20 15:11
 * @Copyright © 2020 中控（西安）
 */
@Data
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "/task-scheduler/v1")
public class WebSchedulerJobLogController extends BaseController {
    @Autowired
    private SchedulerJobLogService schedulerJobLogService;

    @GetMapping("/log/getJobLog")
    public PageResult getSchedulerJobLog(SchedulerJobLogVo schedulerJobLogVo) {
        SchedulerJobLogBo schedulerJobLogBo = new SchedulerJobLogBo();
        BeanUtils.copyProperties(schedulerJobLogVo, schedulerJobLogBo);
        //        String tenantId = RpcContext.getContext().getTenantId();
        //        schedulerJobLogBo.setUserName(tenantId);
        Page<SchedulerJobLogPo> schedulerJobLogs = schedulerJobLogService.getSchedulerJobLog(schedulerJobLogBo);
        List<SchedulerJobLogVo> schedulerJobLogVos = schedulerJobLogs.getRecords().stream().map(schedulerJobLogPo -> {
            SchedulerJobLogVo schedulerJobLog = new SchedulerJobLogVo();
            BeanUtils.copyProperties(schedulerJobLogPo, schedulerJobLog);
            schedulerJobLog.setCreateTime(SchedulerJobLogVo.parseToDate(schedulerJobLogPo.getCreateTime()));
            return schedulerJobLog;
        }).collect(Collectors.toList());
        return new PageResult(schedulerJobLogVos, schedulerJobLogs.getTotal(), schedulerJobLogs.getSize(), schedulerJobLogs.getCurrent());
    }

}
