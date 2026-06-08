/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.listener.process;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.po.ProcessAttentionPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.dao.ProcessAttentionMapper;
import com.supcon.supfusion.flow.dao.ProcessMapper;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessCompleteListener;

/**
 * @author: zhuangmh
 * @date: 2020年11月10日 下午7:44:03
 */
@Component
public class ProcessCompleteListener extends AbstractProcessCompleteListener {

    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private ProcessAttentionMapper processAttentionMapper;
    /**
     * @see com.supcon.supfusion.flow.engine.server.listener.AbstractProcessCompleteListener#deleteProcess(java.lang.String)
     */
    @Override
    public void updateProcess(String processId) {
        ProcessPO processPO = new ProcessPO();
        processPO.setId(Long.parseLong(processId));
        processPO.setProcessStatus(ProcessStatusEnum.COMPLETED.getStatus());
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        String formatDate = sdf.format(new Date());
        processPO.setCompleteTime(formatDate);
        processMapper.updateById(processPO);
        LambdaQueryWrapper<ProcessAttentionPO> queryWrapper = Wrappers.<ProcessAttentionPO>lambdaQuery();
        queryWrapper.eq(ProcessAttentionPO::getProcessId, processId);
        processAttentionMapper.delete(queryWrapper);
    }

}
