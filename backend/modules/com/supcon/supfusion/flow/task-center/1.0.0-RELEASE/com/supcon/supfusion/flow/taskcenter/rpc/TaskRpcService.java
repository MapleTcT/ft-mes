/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.flow.api.TaskServiceApi;
import com.supcon.supfusion.flow.api.dto.TaskTotalsDTO;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;
/**
 * @author: zhuangmh
 * @date: 2021年1月14日 下午3:21:33
 */
@ServiceApiService
public class TaskRpcService implements TaskServiceApi {

    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    /**
     * @see com.supcon.supfusion.flow.api.TaskServiceApi#getTaskTotal(java.util.List)
     */
    @Override
    public TaskTotalsDTO getTaskTotal(List<Long> userIds) {
        TaskTotalsDTO taskTotalsDTO = new TaskTotalsDTO();
        List<TaskTotalsDTO.TaskTotal> taskTotals = new LinkedList<>();
        for (Long userId : userIds) {
            Integer total = pendingTaskMapper.selectCount(new QueryWrapper<PendingTaskPO>().lambda().eq(PendingTaskPO::getUserId, userId));
            if (total.intValue() > 0) {
                taskTotalsDTO.setContainTask(true);
            }
            taskTotals.add(new TaskTotalsDTO.TaskTotal(userId, total.intValue()));
        }
        taskTotalsDTO.setList(taskTotals);
        return taskTotalsDTO;
    }

    @Override
    public Result<Boolean> verificationProcessOwner(Long userId, Long pendingId) {
        LambdaQueryWrapper<PendingTaskPO> pendingTaskPOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pendingTaskPOLambdaQueryWrapper.eq(PendingTaskPO::getUserId, userId);
        pendingTaskPOLambdaQueryWrapper.eq(PendingTaskPO::getId, pendingId);
        Integer count = pendingTaskMapper.selectCount(pendingTaskPOLambdaQueryWrapper);
        return new Result<>(count != null && count > 0);
    }
}
