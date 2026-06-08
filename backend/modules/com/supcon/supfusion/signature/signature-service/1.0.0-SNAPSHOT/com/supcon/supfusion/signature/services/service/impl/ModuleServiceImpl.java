package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.entity.Module;
import com.supcon.supfusion.signature.dao.mappers.ModuleMapper;
import com.supcon.supfusion.signature.services.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
@Transactional
public class ModuleServiceImpl extends ServiceImpl<ModuleMapper,Module> implements ModuleService {

    @Autowired
    ModuleMapper moduleMapper;

    @Override
    @Transactional
    public List<Module> getAllModule() {
        return moduleMapper.selectList(new QueryWrapper<Module>().lambda().eq(Module::getIsHide,false));
    }

}
