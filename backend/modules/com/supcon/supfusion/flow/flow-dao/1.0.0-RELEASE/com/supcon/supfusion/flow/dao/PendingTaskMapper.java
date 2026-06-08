/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午5:01:28
 */
public interface PendingTaskMapper extends BaseMapper<PendingTaskPO> {

    
    @Select("<script>"
            + "SELECT " +
            "    tp.user_id userId, " +
            "    tp.person_name personName, " +
            "    tp.cid, " +
            "    tp.multi_company multiCompany, " +
            "    tp.open_url openUrl, " +
            "    tp.id, " +
            "    tp.task_source taskSource, " +
            "    tp.instance_id instanceId, " +
            "    tp.process_id processId, " +
            "    tp.process_name processName, " +
            "    tp.activity_name activityName, " +
            "    tp.task_description_zh_cn taskDescriptionZhCn, " +
            "    tp.task_description taskDescription, " +
            "    tp.process_description processDescription, " +
            "    tp.table_no tableNo, " +
            "    tp.initiator_id initiatorId, " +
            "    tp.staff_name staffName, " +
            "    tf.form_data formData, " +
            "    start_time startTime, " +
            "    tp.task_status taskStatus " +
            "FROM " +
            "    wfm_task_pending tp " +
            "LEFT JOIN wfm_task_form tf ON " +
            "    tp.instance_id = tf.instance_id AND tp.user_id=tf.user_id " +
            "WHERE " +
            "    tp.process_id = #{processId}" +
            "    order by tp.create_time desc   " +
            "</script>")
    List<PendingTaskPO> queryTaskByProcess(@Param("processId") String processId);
    
    @Select("<script>"
            + "SELECT " +
            "    id, " +
            "    cid, " +
            "    open_url openUrl, " +
            "    multi_company multiCompany, " +
            "    task_source taskSource, " +
            "    process_id processId, " +
            "    process_description processDescription, " +
            "    task_description_zh_cn taskDescriptionZhCn, " +
            "    task_description taskDescription, " +
            "    activity_name activityName, " +
            "    process_name processName, " +
            "    table_no formNo, " +
            "    start_time startTime, " +
            "    task_status taskStatus " +
            "FROM " +
            "wfm_task_pending " +
            "WHERE " +
            "    instance_id = #{instanceId}" +
            "</script>")
    List<PendingTaskPO> getTaskInstances(@Param("instanceId") String instanceId);

    @Select("<script>" +
            "SELECT count(*) from (SELECT " +
            " MAX(tp.id) maxId " +
            "FROM " +
            "    wfm_task_pending tp " +
            "WHERE " +
            "   tp.user_id = #{userId} group by " +
            "   <if test='category == \"task\"'> tp.activity_name ) t</if>" +
            "   <if test='category == \"process\"'> tp.process_key ) t</if>" +
            "</script>")
    int countStatistic(@Param("category") String category, @Param("userId") Long userId);

    @Delete("delete from wfm_task_pending where id=#{task.id}")
    int deleteTask(@Param("task") PendingTaskPO task);
    
}
