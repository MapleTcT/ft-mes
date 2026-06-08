package com.supcon.supfusion.scheduler.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobBo;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public interface SchedulerJobService {

    SchedulerJobBo JobPoToJobModel(SchedulerJobPo job);

    Page scheduleGetAll(SchedulerJobBo schedulerJobBo);

    JobDataMap JobModel2JobDataMap(SchedulerJobBo schedulerJobBo);

    SchedulerJobPo scheduleGetById(Long id);

    void scheduleAdd(SchedulerJobBo schedulerJobBo) throws SchedulerException;

    void updateCallNoAndTriggerDate(Long id);


    void scheduleUpdateTrigger(SchedulerJobBo schedulerJobBo);

    void scheduleUpdateJob(SchedulerJobBo schedulerJobBo);

    void schedulePause(List<Long> list);

    void scheduleResume(List<Long> list);

    void scheduleDelete(List<Long> list);

    String schedulerImmediateExcute(SchedulerJobBo schedulerJobBo) throws Exception;

    Collection<ModuleDTO> queryModules(String keyword, Boolean isAccurate);
}
