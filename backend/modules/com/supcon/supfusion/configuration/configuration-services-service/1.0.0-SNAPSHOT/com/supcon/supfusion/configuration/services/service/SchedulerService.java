package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobDTO;

import java.util.List;

/**
 * @Author kk.C
 * @Description: 调度相关
 * @Date 2021/1/4 13:07
 */
public interface SchedulerService {
    List<SchedulerJobDTO> importXml(String xml);
}
