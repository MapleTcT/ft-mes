/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.po.ProcessLogPO;
import com.supcon.supfusion.flow.dao.ProcessLogMapper;

/**
 * @author: zhuangmh
 * @date: 2020年6月10日 上午10:14:04
 */
@Component
public class LoggerJob extends JobExecutor<ProcessLogPO> {
    
    @Autowired
    private ProcessLogMapper loggerMapper;
    
    /**
     * 记录流程处理日志
     */
    @Override
    public void submit(ProcessLogPO processLog) {
        JOB_THREAD_POOL.execute(() -> loggerMapper.insert(processLog));
    }

}
