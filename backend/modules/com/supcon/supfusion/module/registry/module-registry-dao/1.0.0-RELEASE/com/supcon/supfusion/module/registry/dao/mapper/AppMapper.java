/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.module.registry.dao.po.AppPO;

@Mapper
public interface AppMapper extends BaseMapper<AppPO> {
    
    
    @Insert("INSERT INTO " +
    		"    mod_module_app_rel(id, app_id, module_id) " +
    		"VALUES " +
    		"(#{app.id},#{app.appId},#{app.moduleId})")
    void insertapp(@Param("app") AppPO app);
    
    @Delete("DELETE FROM " + 
            "    mod_module_app_rel " + 
            "WHERE " + 
            "    app_id = #{appId}")
    void deleteApp(@Param("appId") String appId);
    
    @Delete("DELETE FROM " + 
            "    mod_module_registry " + 
            "WHERE " + 
            "    module_id not in (select module_id from mod_module_app_rel) AND module_type='BIZ'")
    void deleteAppModule(@Param("appId") String appId);
   
    @Select("SELECT " +
            "    module_id " + 
            "FROM " + 
            "    mod_module_app_rel " + 
            "WHERE " + 
            "    module_id not in (select module_id from mod_module_app_rel where app_id != #{appId}) " +
            "    AND app_id = #{appId}")
    List<String> singleApp(@Param("appId") String appId);
}
