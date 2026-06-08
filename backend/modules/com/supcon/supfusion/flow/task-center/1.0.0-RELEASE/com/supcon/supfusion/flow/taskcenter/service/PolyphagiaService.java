/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.po.EntrustPO;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.dao.EntrustMapper;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.flow.dao.ProcessMapper;

/**
 * 综合类服务
 * @author: zhuangmh
 * @date: 2021年2月23日 上午9:57:09
 */
@Service
public class PolyphagiaService {
    
    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private EntrustMapper entrustMapper;
    
    /**
     * 流程名称修改后,需要将原数据的流程名称也一并修改
     * @param processKey
     * @param version
     * @param processName
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void syncChangeProcessName(String processKey, Integer version, String processName) {
        // 更新待办
        QueryWrapper<PendingTaskPO> updateTaskWrapper = new QueryWrapper<>();
        updateTaskWrapper.eq(Constants.COL_DIAGRAM_CODE, processKey);
        updateTaskWrapper.eq(Constants.COL_VERSION, version);
        PendingTaskPO taskPo = new PendingTaskPO();
        taskPo.setProcessName(processName);
        pendingTaskMapper.update(taskPo, updateTaskWrapper);
        // 更新我的发起
        LambdaQueryWrapper<ProcessPO> updateProcessWrapper = new QueryWrapper<ProcessPO>().lambda();
        updateProcessWrapper.eq(ProcessPO::getProcessKey, processKey);
        updateProcessWrapper.eq(ProcessPO::getProcessVersion, version);
        updateProcessWrapper.in(ProcessPO::getProcessStatus, ProcessStatusEnum.ACTIVED.getStatus(), ProcessStatusEnum.SUSPENDED.getStatus());
        List<ProcessPO> processes = processMapper.selectList(updateProcessWrapper);
        for (ProcessPO process : processes) {
            ProcessPO entity = new ProcessPO();
            entity.setId(process.getId());
            entity.setProcessName(processName);
            processMapper.updateById(entity);
            // 更新委托记录
            LambdaQueryWrapper<EntrustPO> updateEntrustWrapper = new QueryWrapper<EntrustPO>().lambda();
            updateEntrustWrapper.eq(EntrustPO::getProcessId, process.getId());
            EntrustPO entrust = new EntrustPO();
            entrust.setProcessName(processName);
            entrustMapper.update(entrust, updateEntrustWrapper);
        }
    }
}
