package com.supcon.supfusion.scheduler.server.api.service;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.scheduler.server.api.constants.Constants;
import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.supcon.supfusion.scheduler.server.api.constants.Constants.API_PREFIX;

import java.util.List;
import java.util.Map;

@FeignClient(name = "task-scheduler", contextId = "schedulerJob")
public interface ISchedulerJobApiService {
    /**
     * 获取任务
     *
     * @return
     */
    @GetMapping(API_PREFIX + Constants.JOB_PATH + "/gets")
    Result scheduleGetAll(@ModelAttribute SchedulerJobDTO schedulerJobDTO);

    /**
     * 新增
     *
     * @param schedulerJobDTOList
     * @return
     */
    @PostMapping(API_PREFIX + Constants.JOB_PATH + "/add")
    Result scheduleAdd(@Validated @RequestBody List<SchedulerJobDTO> schedulerJobDTOList);

    /**
     * 更新任务
     *
     * @param schedulerJobDTO
     * @return
     */
    @PutMapping(API_PREFIX + Constants.JOB_PATH + "/update")
    void scheduleUpdateJob(@RequestBody SchedulerJobDTO schedulerJobDTO);

    /**
     * 更新触发器
     *
     * @param schedulerJobDTO
     * @return
     */
    @PutMapping(API_PREFIX + Constants.JOB_PATH + "/updateTrigger")
    void scheduleUpdateTrigger(@RequestBody SchedulerJobDTO schedulerJobDTO);

    /**
     * 批量删除
     *
     * @param
     * @return
     */
    @DeleteMapping(API_PREFIX + Constants.JOB_PATH + "/delete")
    void scheduleDelete(@RequestBody Map<String, Object> params);

    /**
     * 批量暂停任务
     *
     * @param params
     * @return
     */
    @PostMapping(API_PREFIX + Constants.JOB_PATH + "/pause")
    void schedulePause(@RequestBody Map<String, Object> params);

    /**
     * 批量恢复任务
     *
     * @param params
     * @return
     */
    @PostMapping(API_PREFIX + Constants.JOB_PATH + "/resume")
    void scheduleResume(@RequestBody Map<String, Object> params);

    /**
     * 一键执行
     *
     * @param schedulerJobDTO
     * @return
     */
    @PostMapping(API_PREFIX + Constants.JOB_PATH + "/immediateExcute")
    Result scheduleResume(@RequestBody SchedulerJobDTO schedulerJobDTO);

    /**
     * 获取任务
     *
     * @return
     */
    @GetMapping(API_PREFIX + Constants.JOB_PATH + "/get")
    Result<List<SchedulerJobDTO>> getScheduleByModuleCode(@RequestParam(value = "moduleCode")String moduleCode);

}
