package com.supcon.supfusion.scheduler.server.webapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.*;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobBo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import com.supcon.supfusion.scheduler.server.service.exception.TaskErrorEnum;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.scheduler.server.webapi.vo.SchedulerJobVo;
import lombok.Data;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Data
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "task-scheduler/v1")
public class WebapiSchedulerJobController extends BaseController {
    @Autowired
    private SchedulerJobService schedulerJobService;

    /**
     * 获取任务
     *
     * @return
     */
    @GetMapping("/job/gets")
    public PageResult scheduleGetAll(SchedulerJobVo schedulerJobVo) {
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
    public void scheduleAdd(@Validated @RequestBody SchedulerJobVo schedulerJobVo) throws SchedulerException {
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
        SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(schedulerJobVo, schedulerJobBo);
        String tenantId = getUserName(schedulerJobVo);
        schedulerJobBo.setUserName(tenantId);
        schedulerJobService.scheduleUpdateJob(schedulerJobBo);
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
        if (null == ids || ids.size() == 0) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        schedulerJobService.scheduleDelete(ids);
    }

    /**
     * 批量暂停任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/pause")
    public void schedulePause(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) (List) params.get("key");
        if (null == ids || ids.size() == 0) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        schedulerJobService.schedulePause(ids);
    }

    /**
     * 批量恢复任务
     *
     * @param params
     * @return
     */
    @PostMapping("/job/resume")
    public void scheduleResume(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) (List) params.get("key");
        if (null == ids || ids.size() == 0) {
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
     * 获取所有模块信息
     * @return 模块列表
     */
    @GetMapping("/job/queryModules")
    public ListResult<ModuleDTO> scheduleResume(@RequestParam(value = "keyword", required = false)String keyword,@RequestParam(value = "isAccurate", required = false)Boolean isAccurate) throws Exception {
        Collection<ModuleDTO> moduleDTOs = schedulerJobService.queryModules(keyword,isAccurate);
        ListResult<ModuleDTO> listResult = new ListResult<> (moduleDTOs);
        listResult.setMessage("success");
        listResult.setCode(200);
        return listResult;
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
//        return "lw";
    }

}
