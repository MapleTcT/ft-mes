/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.mybatis;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.supcon.supfusion.flow.common.dto.ProcessQueryContractDTO;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.po.ProcessLogPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;

/**
 * @author: zhuangmh
 * @date: 2020年6月5日 下午3:35:03
 */
public class ProcessQueryWrapper {
    
    private ProcessQueryWrapper() {
        throw new IllegalStateException("ProcessQueryWrapper is utility class, do not instantiate");
    }
    
    public static LambdaQueryWrapper<ProcessLogPO> buildProcessLogQueryWrapper(String processId) {
        return Wrappers.<ProcessLogPO>lambdaQuery()
                .eq(ProcessLogPO::getProcessId, processId)
                .orderByDesc(ProcessLogPO::getCreateTime);
    }
    
    public static LambdaQueryWrapper<ProcessPO> buildMyProcessQueryWrapper(ProcessQueryContractDTO queryContract, Long userId, String tenantId) {
        LambdaQueryWrapper<ProcessPO> queryWrapper = Wrappers.<ProcessPO>lambdaQuery()
                .eq(ProcessPO::getUserId, userId);
        if (queryContract.getProcessNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getProcessNames(), ProcessPO::getProcessName);
        }
        if (queryContract.getVersions() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getVersions(), ProcessPO::getProcessVersion);
        }
        if (queryContract.getStartFrom() != null) {
            queryWrapper.and(i -> i.ge(ProcessPO::getCreateTime, queryContract.getStartFrom()));
        }
        if (queryContract.getStartTo() != null) {
            queryWrapper.and(i -> i.le(ProcessPO::getCreateTime, queryContract.getStartTo()));
        }
        if (queryContract.getAppId() != null) {
            queryWrapper.and(i -> i.le(ProcessPO::getAppId, queryContract.getAppId()));
        }
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(ProcessPO::getTenantId, tenantId));
        }
        List<Integer> processStatus = new ArrayList<>();
        processStatus.add(ProcessStatusEnum.ACTIVED.getStatus());
        processStatus.add(ProcessStatusEnum.SUSPENDED.getStatus());
        queryWrapper.in(ProcessPO::getProcessStatus, processStatus);
        return queryWrapper;
    }
    
    public static LambdaQueryWrapper<ProcessPO> buildProcessQueryWrapper(ProcessQueryContractDTO queryContract, String tenantId) {
        LambdaQueryWrapper<ProcessPO> queryWrapper = Wrappers.<ProcessPO>lambdaQuery();
        if (queryContract.getIds() != null) {
            queryWrapper.in(ProcessPO::getId, queryContract.getIds());
        }
        if (queryContract.getProcessNames() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getProcessNames(), ProcessPO::getProcessName);
        }
        if (queryContract.getVersions() != null) {
            queryWrapper.in(ProcessPO::getProcessVersion, queryContract.getVersions());
        }
        if (queryContract.getInitiators() != null) {
            queryWrapperBuilder(queryWrapper, queryContract.getInitiators(), ProcessPO::getStaffName);
        }
        List<Integer> processStatus = new ArrayList<>();
        processStatus.add(ProcessStatusEnum.ACTIVED.getStatus());
        processStatus.add(ProcessStatusEnum.SUSPENDED.getStatus());
        queryWrapper.in(ProcessPO::getProcessStatus, processStatus);
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(ProcessPO::getTenantId, tenantId));
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
