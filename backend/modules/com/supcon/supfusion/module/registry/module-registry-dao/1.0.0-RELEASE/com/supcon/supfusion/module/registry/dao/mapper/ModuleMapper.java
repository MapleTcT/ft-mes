/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.module.registry.dao.po.ModulePO;

/**
 * @author: zhuangmh
 * @date: 2020年7月11日 下午4:52:01
 */
@Mapper
public interface ModuleMapper extends BaseMapper<ModulePO> {
    
    @Select("<script>" +
            "SELECT " + 
            "    id, " + 
            "    module_id moduleId, " + 
            "    module_code moduleCode, " + 
            "    module_name moduleName, " + 
            "    module_type moduleType " + 
            "FROM " + 
            "    mod_module_registry " + 
            "    <if test='moduleType != null'> WHERE module_type = #{moduleType} </if>" +
            "    ORDER BY module_id ASC" +
            "</script>")
    List<ModulePO> selectModules(@Param("moduleType")String moduleType);
    
    @Select("SELECT " + 
            "    id, " + 
            "    module_id moduleId, " + 
            "    module_code moduleCode, " + 
            "    module_name moduleName, " + 
            "    module_type moduleType " + 
            "FROM " + 
            "    mod_module_registry " + 
            "WHERE " + 
            "    module_id = #{moduleId} ")
    ModulePO getOne(@Param("moduleId")String moduleId);
    
    @Select("SELECT " + 
            "    id, " + 
            "    module_id moduleId, " + 
            "    module_code moduleCode, " + 
            "    module_name moduleName, " + 
            "    module_type moduleType " + 
            "FROM " + 
            "    mod_module_registry " + 
            "WHERE " + 
            "    module_name = #{moduleName}")
    ModulePO getByName(@Param("moduleName")String moduleName);
    
    @Delete("DELETE FROM " + 
            "    mod_module_registry " + 
            "WHERE " + 
            "    module_id = #{moduleId} " + 
            "    AND module_type = 'BIZ' ")
    int deleteOne(@Param("moduleId")String moduleId);
    
    @Update("UPDATE " +
    		"    mod_module_registry " +
    		"SET " +
    		"    module_code = #{moduleCode} , " +
    		"    module_name = #{moduleName} " +
    		"WHERE " +
    		"    module_id = #{moduleId}")
    void update(@Param("moduleId") String moduleId , @Param("moduleCode") String moduleCode , @Param("moduleName") String moduleName);
}
