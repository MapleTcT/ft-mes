/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.flow.common.po.TaskFormPO;

/**
 * @author: zhuangmh
 * @date: 2020年8月28日 下午2:44:19
 */
public interface TaskFormMapper extends BaseMapper<TaskFormPO> {

    @Delete("DELETE " + 
            "FROM " + 
            "    wfm_task_form " + 
            "WHERE " + 
            "    instance_id = #{instanceId} ")
    void deleteFormData(@Param("instanceId") String instanceId);
}
