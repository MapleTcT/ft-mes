/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.mybatis;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.supcon.supfusion.flow.common.dto.CompleteTaskQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.EntrustQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.PendingQueryContractDTO;
import com.supcon.supfusion.flow.common.po.CompleteTaskPO;
import com.supcon.supfusion.flow.common.po.EntrustPO;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午5:54:35
 */
public class TaskQueryWrapper {
    
    private TaskQueryWrapper() {
        throw new IllegalStateException("ProcessQueryWrapper is utility class, do not instantiate");
    }
    
    /*
     * 根据taskId查询
     */
    public static LambdaQueryWrapper<PendingTaskPO> buildUniqueQueryWrapper(Long taskId, Long userId) {
        return Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getId, taskId)
                .eq(PendingTaskPO::getUserId, userId);
    }
    
    public static LambdaQueryWrapper<PendingTaskPO> buildIdQueryWrapper(Long taskId) {
        return Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getId, taskId);
    }
    
    /*
     * 根据processId查询
     */
    public static LambdaQueryWrapper<PendingTaskPO> buildProcessIdQueryWrapper(String processId, String tenantId) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getProcessId, processId);
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(PendingTaskPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }
    
    /*
     * 查询委托记录
     */
    public static LambdaQueryWrapper<EntrustPO> buildEntrustQueryWrapper(Long taskId, Long principal, Long mandatary) {
        LambdaQueryWrapper<EntrustPO> queryWrapper = Wrappers.<EntrustPO>lambdaQuery()
                .eq(EntrustPO::getTaskId, taskId);
        if (mandatary != null) {
            queryWrapper.and(i -> i.eq(EntrustPO::getMandatary, mandatary));
        }
        if (principal != null) {
            queryWrapper.and(i -> i.eq(EntrustPO::getPrincipal, principal));
        }
        return queryWrapper;
    }
    
    public static LambdaQueryWrapper<PendingTaskPO> buildTaskListQueryWrapper(PendingQueryContractDTO queryContract, String tenantId) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery();
        if (queryContract.getAppId() != null) {
            queryWrapper.eq(PendingTaskPO::getAppId, queryContract.getAppId());
        }
        if (queryContract.getIds() != null) {
            queryWrapper.and(i -> i.in(PendingTaskPO::getId, queryContract.getIds()));
        }
        if (queryContract.getStartFrom() != null) {
            queryWrapper.and(i -> i.ge(PendingTaskPO::getCreateTime, queryContract.getStartFrom()));
        }
        if (queryContract.getStartTo() != null) {
            queryWrapper.and(i -> i.le(PendingTaskPO::getCreateTime, queryContract.getStartTo()));
        }
        if (queryContract.getFormNos() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getFormNos(), PendingTaskPO::getTableNo);
        }
        if (queryContract.getStatus() != null) {
            queryWrapper.and(i -> i.in(PendingTaskPO::getTaskStatus, queryContract.getStatus()));
        }
        if (queryContract.getInitiators() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getInitiators(), PendingTaskPO::getStaffName);
        }
        if (queryContract.getProcessNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getProcessNames(), PendingTaskPO::getProcessName);
        }
        if (queryContract.getTaskNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getTaskNames(), PendingTaskPO::getTaskDescriptionZhCn);
        }
        if (queryContract.getAssignees() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getAssignees(), PendingTaskPO::getPersonName);
        }
        if (queryContract.getVersions() != null) {
            queryWrapper.and(i -> i.in(PendingTaskPO::getProcessVersion, queryContract.getVersions()));
        }
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(PendingTaskPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }
    
    public static LambdaQueryWrapper<CompleteTaskPO> buildCompleteTaskListQueryWrapper(CompleteTaskQueryContractDTO queryContract, String tenantId) {
        LambdaQueryWrapper<CompleteTaskPO> queryWrapper = Wrappers.<CompleteTaskPO>lambdaQuery();
        if (queryContract.getAppId() != null) {
            queryWrapper.eq(CompleteTaskPO::getAppId, queryContract.getAppId());
        }
        if (queryContract.getStartFrom() != null) {
            queryWrapper.and(i -> i.ge(CompleteTaskPO::getStartTime, queryContract.getStartFrom()));
        }
        if (queryContract.getStartTo() != null) {
            queryWrapper.and(i -> i.le(CompleteTaskPO::getStartTime, queryContract.getStartTo()));
        }
        if (queryContract.getFormNos() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getFormNos(), CompleteTaskPO::getTableNo);
        }
        if (queryContract.getInitiators() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getInitiators(), CompleteTaskPO::getStaffName);
        }
        if (queryContract.getProcessNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getProcessNames(), CompleteTaskPO::getProcessName);
        }
        if (queryContract.getTaskNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getTaskNames(), CompleteTaskPO::getTaskName);
        }
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(CompleteTaskPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }
    
    /*
     * 查询委托列表
     */
    public static LambdaQueryWrapper<EntrustPO> buildEntrustListQueryWrapper(EntrustQueryContractDTO queryContract, String tenantId) {
        LambdaQueryWrapper<EntrustPO> queryWrapper = Wrappers.<EntrustPO>lambdaQuery()
                .eq(EntrustPO::getPrincipal, queryContract.getPrincipal());
        if (queryContract.getAppId() != null) {
            queryWrapper.eq(EntrustPO::getAppId, queryContract.getAppId());
        }
        if (queryContract.getTaskNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getTaskNames(), EntrustPO::getTaskName);
        }
        if (queryContract.getProcessNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getProcessNames(), EntrustPO::getProcessName);
        }
        if (queryContract.getMandatarys() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getMandatarys(), EntrustPO::getMandataryName);
        }
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(EntrustPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }

    private  static <T> void queryWrapperBuilder(LambdaQueryWrapper<T> queryWrapper, List<String> condition, SFunction<T,?> column) {
        queryWrapper.and(i -> {
            int size = condition.size();
            for (int t = 0; t < size; t++) {
                if (t > 0) {
                    i.or();
                }
                i.like(column, condition.get(t)).or();
            }
        });
    }
}
