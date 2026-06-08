package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.Entity;
import com.supcon.supfusion.custon.property.dao.mappers.EntityMapper;
import com.supcon.supfusion.custon.property.server.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhang yafei
 */
@Service
public class EntityServiceImpl implements EntityService {

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
