package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.Module;
import com.supcon.supfusion.custon.property.dao.entity.ModuleRelation;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleMapper;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleReferenceMapper;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleRelationMapper;
import com.supcon.supfusion.custon.property.server.ModuleServiceFoundation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhang yafei
 */
@Service
public class ModuleServiceFoundationImpl implements ModuleServiceFoundation {

    @Autowired
    ModuleMapper moduleMapper;

    @Autowired
    ModuleRelationMapper moduleRelationMapper;

    @Autowired
    ModuleReferenceMapper moduleReferenceMapper;


    @Override
    @Transactional
    public List<Module> getAllModule() {
        return moduleMapper.selectList(new QueryWrapper<Module>().lambda().eq(Module::getIsHide,false));
    }

    @Override
    public Module getModuleByCode(String code) {
        return  moduleMapper.selectById(code);
    }

    @Override
    public List<Module> getModuleRelaton(String moduleCode) {
        List<Module> modules = moduleRelationMapper.selectTargetModuleByModuleCode(moduleCode);
        return modules;
    }

    @Override
    public List<Module> getReferences(String moduleCode) {
        return moduleReferenceMapper.selectTargetModuleByModuleCode(moduleCode);
    }
}
