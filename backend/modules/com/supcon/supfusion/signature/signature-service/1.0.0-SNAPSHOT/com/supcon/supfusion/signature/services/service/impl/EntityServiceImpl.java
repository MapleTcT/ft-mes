package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.entity.Entity;
import com.supcon.supfusion.signature.dao.entity.Module;
import com.supcon.supfusion.signature.dao.mappers.EntityMapper;
import com.supcon.supfusion.signature.services.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhang yafei
 */
@Service
public class EntityServiceImpl extends ServiceImpl<EntityMapper,Entity> implements EntityService {

    @Autowired
    private EntityMapper entityMapper;

    @Override
    @Transactional
    public List<Entity> findEntities(String moduleCode) {
        LambdaQueryWrapper<Entity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Entity::getModuleCode,moduleCode);
        wrapper.orderByAsc(Entity::getCode);

        return entityMapper.selectList(wrapper);
    }

}
