/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.exception.PermissionException;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.po.TaskFormPO;
import com.supcon.supfusion.flow.common.util.CodeGenerator;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.flow.dao.TaskFormMapper;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;

/**
 * @author: zhuangmh
 * @date: 2020年9月1日 下午7:04:58
 */
@Service
public class FormService {

    @Autowired
    private TaskFormMapper taskFormMapper;
    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    
    /**
     * 保存表单数据
     */
    public void saveForm(Long taskId, String formJson) {
        Long userId = UserContext.getUserContext().getUserId();
        PendingTaskPO pendingTask = pendingTaskMapper.selectById(taskId);
        if (pendingTask == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        if (pendingTask.getUserId().longValue() != userId.longValue()) {
            throw new PermissionException(FlowErrorEnum.TASK_NO_SUBMIT_PERMISSION_ERROR);
        }
        LambdaQueryWrapper<TaskFormPO> queryWrapper = new QueryWrapper<TaskFormPO>().lambda()
                .eq(TaskFormPO::getUserId, userId)
                .eq(TaskFormPO::getInstanceId, pendingTask.getInstanceId());
        TaskFormPO formData = taskFormMapper.selectOne(queryWrapper);
        if (formData == null) {
            create(userId, pendingTask.getInstanceId(), pendingTask.getProcessId(), formJson);
        } else {
            TaskFormPO updateData = new TaskFormPO();
            updateData.setId(formData.getId());
            updateData.setFormTempData(formJson);
            taskFormMapper.updateById(updateData);
        }
    }
    
    private void create(Long userId, String instanceId, String processId, String formJson) {
        TaskFormPO taskForm = new TaskFormPO();
        taskForm.setId(CodeGenerator.generateUUID());
        taskForm.setProcessId(processId);
        taskForm.setFormData(formJson);
        taskForm.setUserId(userId);
        taskForm.setInstanceId(instanceId);
        taskForm.setCreator(UserContext.getUserContext().getUserName());
        taskForm.setCreateStaffId(UserContext.getUserContext().getStaffId());
        taskFormMapper.insert(taskForm);
    }
    
    /**
     * 重置表单数据
     * @param userId
     * @param taskId
     */
    public void resetForm(Long taskId) {
        Long userId = UserContext.getUserContext().getUserId();
        PendingTaskPO pendingTask = pendingTaskMapper.selectById(taskId);
        if (pendingTask == null) {
            throw new NotExistException(FlowErrorEnum.TASK_NOT_EXIST_ERROR);
        }
        QueryWrapper<TaskFormPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.COL_USER_ID, userId);
        queryWrapper.eq(Constants.COL_INSTANCE_ID, pendingTask.getInstanceId());
        TaskFormPO formData = taskFormMapper.selectOne(queryWrapper);
        if (formData == null) {
            return;
        }
        TaskFormPO newFormData = new TaskFormPO();
        newFormData.setId(formData.getId());
        newFormData.setFormTempData("");
        taskFormMapper.updateById(newFormData);
    }
    
    /**
     * 删除表单数据
     * @param processIds
     */
    public void deleteForm(List<String> processIds) {
        LambdaQueryWrapper<TaskFormPO> queryWrapper = new QueryWrapper<TaskFormPO>().lambda().in(TaskFormPO::getProcessId, processIds);
        taskFormMapper.delete(queryWrapper);
    }
}
