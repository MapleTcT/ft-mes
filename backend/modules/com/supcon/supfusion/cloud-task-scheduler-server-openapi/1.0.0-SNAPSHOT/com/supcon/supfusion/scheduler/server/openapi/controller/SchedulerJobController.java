package com.supcon.supfusion.scheduler.server.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import com.supcon.supfusion.scheduler.server.openapi.vo.SchedulerJobVo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobBo;
import com.supcon.supfusion.scheduler.server.service.exception.TaskErrorEnum;
import lombok.Data;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "task-scheduler/v1")
public class SchedulerJobController extends BaseController {

    @Autowired
    private SchedulerJobService schedulerJobService;


    /**
     * 获取任务
     *
     * @return
     */
    @GetMapping("/job/gets")
    public PageResult scheduleGetAll(@ModelAttribute SchedulerJobVo schedulerJobVo) {
        SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
        //        String tenantId = getUserName(schedulerJobVo);
        //        schedulerJobBo.setUserName(tenantId);
        Page<SchedulerJobPo> schedulerJobPoPage = schedulerJobService.scheduleGetAll(schedulerJobBo);
        List<SchedulerJobVo> schedulerJobVos = schedulerJobPoPage.getRecords().stream().map(schedulerJobPo -> {
            SchedulerJobVo schedulerJob = new SchedulerJobVo();
            BeanUtils.copyProperties(schedulerJobPo, schedulerJob);
            return schedulerJob;
        }).collect(Collectors.toList());
        return new PageResult(schedulerJobVos, schedulerJobPoPage.getTotal(), schedulerJobPoPage.getSize(), schedulerJobPoPage.getCurrent());
    }

    /**
     * 新增
     *
     * @param schedulerJobVo
     * @return
     */
    @PostMapping("/job/add")
    public void scheduleAdd(@RequestBody SchedulerJobVo schedulerJobVo) throws SchedulerException {
        SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
        String tenantId = getUserName(schedulerJobVo);
        schedulerJobBo.setUserName(tenantId);
        schedulerJobService.scheduleAdd(schedulerJobBo);
    }

    /**
     * 更新任务
     *
     * @param schedulerJobVo
     * @return
     */
    @PutMapping("/job/update")
    public void scheduleUpdateJob(@RequestBody SchedulerJobVo schedulerJobVo) {
        try {
            SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
            BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
            String tenantId = getUserName(schedulerJobVo);
            schedulerJobBo.setUserName(tenantId);
            schedulerJobService.scheduleUpdateJob(schedulerJobBo);
        } catch (Exception e) {
            throw new BizException(TaskErrorEnum.UPDATE_TASK_FAILURE);
        }
    }

    /**
     * 更新触发器
     *
     * @param schedulerJobVo
     * @return
     */
    @PutMapping("/job/updateTrigger")
    public void scheduleUpdateTrigger(@RequestBody SchedulerJobVo schedulerJobVo) {
        SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
        String tenantId = getUserName(schedulerJobVo);
        schedulerJobBo.setUserName(tenantId);
        schedulerJobService.scheduleUpdateTrigger(schedulerJobBo);
    }

    /**
     * 批量删除定时任务
     *
     * @param
     * @return
     */
    @DeleteMapping("/job/delete")
    public void scheduleDelete(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) (List) params.get("key");
        if(null==ids||ids.size()==0){
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        schedulerJobService.scheduleDelete(ids);
    }

    /**
     * 批量暂停定时任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/pause")
    public void schedulePause(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) (List) params.get("key");
        if(null==ids||ids.size()==0){
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        schedulerJobService.schedulePause(ids);
    }

    /**
     * 批量恢复定时任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/resume")
    public void scheduleResume(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) (List) params.get("key");
        if(null==ids||ids.size()==0){
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }

        schedulerJobService.scheduleResume(ids);
    }


    /**
     * 一键执行
     *
     * @param schedulerJobVo
     * @return
     */
    @PostMapping("/job/immediateExcute")
    public Result scheduleResume(@RequestBody SchedulerJobVo schedulerJobVo) throws Exception {
        SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
        String tenantId = getUserName(schedulerJobVo);
        schedulerJobBo.setUserName(tenantId);
        String s = schedulerJobService.schedulerImmediateExcute(schedulerJobBo);
        return Result.data(200, "success", s);
    }

    /**
     * 获取租户信息
     *
     * @param schedulerJobVo
     * @return
     */

    private String getUserName(SchedulerJobVo schedulerJobVo) {
        String tenantId = RpcContext.getContext().getTenantId();
        return tenantId;
    }



}
