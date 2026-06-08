/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.taskcenter.service.TaskCenterService;

/**
 * 待办任务合并, 合并条件如下:
 *  1. 流程实例相同
 *  2. 处于同一个任务环节
 *  3. 单据编号相同
 *  4. 待办执行者相同
 * @author: zhuangmh
 * @date: 2020年6月10日 上午10:14:04
 */
@Component
public class TaskCombineJob extends JobExecutor<String> {
    
    @Autowired
    private TaskCenterService taskCenterService;
    /**
     * 
     */
    @Override
    public void submit(String processId) {
        JOB_THREAD_POOL.execute(() -> {
            try {
                Thread.sleep(2000); // 延迟2秒合并, 为了让当前事务先结束,否则会触发乐观锁异常
            } catch (InterruptedException ignore) {

            }
            taskCenterService.combineTask(processId);
        });
    }

}
