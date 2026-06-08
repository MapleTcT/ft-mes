package com.supcon.supfusion.scheduler.server.api.service;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.scheduler.server.api.vo.SchedulerJobVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "supfusion-task-scheduler-service")
@ServiceApi(path = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "task-scheduler/v1")
@Validated
public interface SchedulerJobServiceApi {
    /**
     * 获取任务
     *
     * @return
     */
    @GetMapping("/job/gets")
    public Result scheduleGetAll(@ModelAttribute SchedulerJobVo schedulerJobVo);

    /**
     * 新增
     *
     * @param schedulerJobVo
     * @return
     */
    @PostMapping("/job/add")
    public void scheduleAdd(@RequestBody SchedulerJobVo schedulerJobVo);


    /**
     * 更新任务
     *
     * @param schedulerJobVo
     * @return
     */
    @PutMapping("/job/update")
    public void scheduleUpdateJob(@RequestBody SchedulerJobVo schedulerJobVo);


    /**
     * 更新触发器
     *
     * @param schedulerJobVo
     * @return
     */
    @PutMapping("/job/updateTrigger")
    public void scheduleUpdateTrigger(@RequestBody SchedulerJobVo schedulerJobVo);

    /**
     * 批量删除
     *
     * @param
     * @return
     */
    @DeleteMapping("/job/delete")
    public void scheduleDelete(@RequestBody Map<String, Object> params);

    /**
     * 批量暂停任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/pause")
    public void schedulePause(@RequestBody Map<String, Object> params);


    /**
     * 批量恢复任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/resume")
    public void scheduleResume(@RequestBody Map<String, Object> params);


    /**
     * 一键执行
     *
     * @param schedulerJobVo
     * @return
     */
    @PostMapping("/job/immediateExcute")
    public Result scheduleResume(@RequestBody SchedulerJobVo schedulerJobVo);


}
