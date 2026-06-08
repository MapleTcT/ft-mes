package com.supcon.supfusion.scheduler.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobLogBo;
import org.springframework.stereotype.Service;

@Service
public interface SchedulerJobLogService {

    void addSchedulerJobLog(SchedulerJobLogPo SchedulerJobLogPo);

    Page<SchedulerJobLogPo> getSchedulerJobLog(SchedulerJobLogBo SchedulerJobLogBo);


}
