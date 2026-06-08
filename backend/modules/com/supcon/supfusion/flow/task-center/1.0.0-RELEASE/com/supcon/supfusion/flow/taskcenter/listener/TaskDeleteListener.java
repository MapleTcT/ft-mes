/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.listener;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.flow.common.dto.TaskDTOAdapter;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.flow.dao.TaskFormMapper;
import com.supcon.supfusion.flow.engine.server.listener.AbstractTaskListener;

/**
 * @author: zhuangmh
 * @date: 2020年6月4日 下午4:40:07
 */
@Component("taskDeleteListener")
public class TaskDeleteListener extends AbstractTaskListener {

    private static final long serialVersionUID = 1L;
    @Autowired
    private transient PendingTaskMapper pendingTaskMapper;
    @Autowired
    private transient TaskFormMapper taskFormMapper;
    
    @Override
    public void createPendingTask(TaskDTOAdapter taskDTOAdapter) {
        // nothing need to do
    }

    /**
     * @see AbstractTaskListener#deletePendingTask(java.lang.String)
     */
    @Override
    public void deletePendingTask(String taskInstanceId) {
        LambdaQueryWrapper<PendingTaskPO> queryWrapper = Wrappers.<PendingTaskPO>lambdaQuery()
                .eq(PendingTaskPO::getInstanceId, taskInstanceId);
        List<PendingTaskPO> allTask = pendingTaskMapper.selectList(queryWrapper);
        for (PendingTaskPO t : allTask) {
            pendingTaskMapper.deleteTask(t);
        }
        taskFormMapper.deleteFormData(taskInstanceId);
    }


}
