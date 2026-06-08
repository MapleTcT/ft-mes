/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午5:54:35
 */
public class TaskUpdateWrapper {
    
    private TaskUpdateWrapper() {
        throw new IllegalStateException("TaskUpdateWrapper is utility class, do not instantiate");
    }
    
    /*
     * 根据taskId查询
     */
    public static LambdaQueryWrapper<PendingTaskPO> buildStatusUpdateWrapper(Long taskId, String processId, String tenantId) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getId, taskId)
                .and(i -> i.eq(PendingTaskPO::getProcessId, processId));
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(PendingTaskPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }
    
}
