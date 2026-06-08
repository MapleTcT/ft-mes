package com.supcon.supfusion.scheduler.server.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobDTO;
import com.supcon.supfusion.scheduler.server.api.service.ISchedulerJobApiService;
import com.supcon.supfusion.scheduler.server.dao.SchedulerJobMapper;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobBo;
import com.supcon.supfusion.scheduler.server.service.exception.TaskErrorEnum;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ServiceApiService
@Slf4j
public class ISchedulerJobApiServiceImpl implements ISchedulerJobApiService {

    @Autowired
    private SchedulerJobService schedulerJobService;
    @Autowired
    private SchedulerJobMapper schedulerJobMapper;

    @Override
    public Result scheduleGetAll(SchedulerJobDTO schedulerJobDTO) {
        return null;
    }

    @Override
    public Result scheduleAdd(List<SchedulerJobDTO> schedulerJobDTOList) {
        Result result = new Result();
        result.setCode(200);
        if (ObjectUtils.isEmpty(schedulerJobDTOList)) {
            return null;
        }
        for (SchedulerJobDTO schedulerJobDTO : schedulerJobDTOList) {
            SchedulerJobBo schedulerJobBo = new SchedulerJobBo();
            BeanUtils.copyProperties(schedulerJobDTO, schedulerJobBo);
            try {
                schedulerJobService.scheduleAdd(schedulerJobBo);
            } catch (Exception e) {
                log.error(e.getMessage());
                result.setCode(TaskErrorEnum.ADD_TASK_FAILURE.getCode());
                result.setMessage(TaskErrorEnum.ADD_TASK_FAILURE.getMessage());
            }
        }
        return result;
    }

    @Override
    public void scheduleUpdateJob(SchedulerJobDTO schedulerJobDTO) {

    }

    @Override
    public void scheduleUpdateTrigger(SchedulerJobDTO schedulerJobDTO) {

    }

    @Override
    public void scheduleDelete(Map<String, Object> params) {

    }

    @Override
    public void schedulePause(Map<String, Object> params) {

    }

    @Override
    public void scheduleResume(Map<String, Object> params) {

    }

    @Override
    public Result scheduleResume(SchedulerJobDTO schedulerJobDTO) {
        return null;
    }

    @Override
    public Result<List<SchedulerJobDTO>> getScheduleByModuleCode(String moduleCode) {
        List<SchedulerJobDTO> schedulerJobDTOS = new ArrayList<>();
        Result result = new Result();
        result.setCode(200);
        QueryWrapper<SchedulerJobPo> qryWrapper = new QueryWrapper<>();
        qryWrapper.eq("module_code", moduleCode);
        List<SchedulerJobPo>schedulerJobPos = schedulerJobMapper.selectList(qryWrapper);
        for (SchedulerJobPo schedulerJobPo : schedulerJobPos) {
            SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
            BeanUtils.copyProperties(schedulerJobPo,schedulerJobDTO);
            schedulerJobDTOS.add(schedulerJobDTO);
        }
        result.setData(schedulerJobDTOS);
        return result;
    }
}
