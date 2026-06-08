/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.flow.common.po.CompleteTaskPO;

/**
 * @author: zhuangmh
 * @date: 2020年8月24日 下午2:31:58
 */
public interface CompleteTaskMapper extends BaseMapper<CompleteTaskPO> {
    
    @Select("<script>"
            + "SELECT " + 
            "    tc.id, " +
            "    tc.cid, " + 
            "    tc.open_url openUrl, " + 
            "    tc.user_id userId, " + 
            "    tc.activity_name activityName, " + 
            "    tc.instance_id instanceId, " + 
            "    tc.source_staff sourceStaff, " +
            "    tc.proxy_source proxySource, " +
            "    tc.task_source taskSource, " + 
            "    tc.process_id processId, " + 
            "    tc.task_name taskName, " + 
            "    tc.process_name processName, " + 
            "    tc.table_no tableNo, " +
            "    tf.form_data formData, " +
            "    tc.latest_user latestUser, " +
            "    tc.staff_name staffName, " + 
            "    start_time startTime, " + 
            "    end_time endTime " + 
            "FROM " + 
            "    wfm_task_complete tc " + 
            "    LEFT JOIN wfm_task_form tf ON tc.instance_id=tf.instance_id AND tc.user_id=tf.user_id " + 
            "WHERE " + 
            "    tc.id = #{taskId}" +
            "</script>")
    CompleteTaskPO getCompleteTask(@Param("taskId")Long taskId);

}
