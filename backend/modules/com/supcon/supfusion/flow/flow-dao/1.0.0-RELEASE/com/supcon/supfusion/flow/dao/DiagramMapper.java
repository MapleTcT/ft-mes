/**
 * Licensed to the Deep Blue SUPCON
 * @author: zhuangmh
 * @date: 2020年5月18日 下午2:01:14
 */
package com.supcon.supfusion.flow.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.flow.common.po.DiagramPO;

/**
 * @author: zhuangmh
 * @date: 2020年5月18日 下午2:01:14
 */
public interface DiagramMapper extends BaseMapper<DiagramPO> {
    
    @Select("<script>"
            + "SELECT" + 
            "    MAX(version) maxVersion " + 
            "FROM" + 
            "    wfm_diagram " + 
            "WHERE " + 
            "    process_key = #{processKey} " + 
            "    AND valid = 1 " +
            " <if test='appId != null'> AND app_id = #{appId} </if>" +
            " <if test='tenantId != null'> AND tenant_id = #{tenantId} </if>"
            + "</script>")
    Integer selectMaxVersion(@Param("appId") String appId, @Param("processKey") String processKey, @Param("tenantId") String tenantId);
    
    @Select("<script>" + 
            "SELECT" + 
            "    cid, " + 
            "    app_id appId, " + 
            "    process_key processKey, " + 
            "    version, " + 
            "    multi_company multiCompany, " + 
            "    published_json publishedJson," + 
            "    draft_json draftJson," + 
            "    process_name processName, " + 
            "    d.creator, " +  
            "    d.create_staff_id createStaffId " +  
            "FROM" + 
            "    wfm_diagram d " + 
            "LEFT JOIN wfm_diagram_content dc ON " + 
            "    d.content_id = dc.id " + 
            "WHERE " + 
            "    process_key = #{processKey} " + 
            "    AND version = #{version} " + 
            "    AND valid = 1 " +
            " <if test='tenantId != null'> AND tenant_id = #{tenantId} </if>" +
            "</script>")
    DiagramPO selectSingle(@Param("processKey") String processKey, @Param("version") int version, @Param("tenantId") String tenantId);
    
    @Select("SELECT" + 
            "    d.*," + 
            "    dc.published_json publishedJson," + 
            "    dc.draft_json draftJson " + 
            "FROM" + 
            "    wfm_diagram d " + 
            "LEFT JOIN wfm_diagram_content dc ON " + 
            "    d.content_id = dc.id " + 
            "WHERE" + 
            "    d.id = #{id} " + 
            "    AND d.valid = 1")
    DiagramPO selectSingleById(@Param("id") long id);
    
    @Select("<script>" +
            "SELECT" +
            "    dc.published_json publishedJson " + 
            "FROM" + 
            "    wfm_diagram d " + 
            "LEFT JOIN wfm_diagram_content dc ON " + 
            "    d.content_id = dc.id " + 
            "WHERE" + 
            "    d.process_key = #{processKey} " + 
            "    AND d.enabled = 1 " +
            " <if test='tenantId != null'> AND d.tenant_id = #{tenantId} </if>" +
            "    AND d.valid = 1" +
            "</script>")
    String selectEnabledDiagram(@Param("processKey") String processKey, @Param("tenantId") String tenantId);
    
    @Select("SELECT" + 
            "    d.id, " +  
            "    d.cid, " +  
            "    d.app_id appId, " +  
            "    d.process_key processKey, " +  
            "    d.process_name processName, " +  
            "    d.version, " +  
            "    d.multi_company multiCompany, " +  
            "    d.start_on_mobile startOnMobile, " + 
            "    d.tenant_id tenantId, " +  
            "    d.creator, " +
            "    d.creator_staff creatorStaff, " +
            "    dc.published_json publishedJson," + 
            "    dc.draft_json draftJson " + 
            "FROM" + 
            "    wfm_diagram d " + 
            "LEFT JOIN wfm_diagram_content dc ON " + 
            "    d.content_id = dc.id " + 
            "WHERE" + 
            "    d.app_id = #{appId} " + 
            "    AND d.enabled = 1 " +
            "    AND d.valid = 1")
    List<DiagramPO> selectListByApp(@Param("appId") String appId);

	
}
