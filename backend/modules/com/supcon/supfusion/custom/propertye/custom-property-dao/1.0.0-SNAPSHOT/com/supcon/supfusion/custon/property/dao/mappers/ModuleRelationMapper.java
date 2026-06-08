package com.supcon.supfusion.custon.property.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.custon.property.dao.entity.Module;
import com.supcon.supfusion.custon.property.dao.entity.ModuleRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zhang yafei
 */
@Mapper
@Repository
public interface ModuleRelationMapper extends BaseMapper<ModuleRelation> {

    @Select(" select m.name,m.code from ec_module m , ec_module_relation r where r.module_code = #{moduleCode} and r.target_module_code =m.code")
    List<Module> selectTargetModuleByModuleCode(String moduleCode);
}
