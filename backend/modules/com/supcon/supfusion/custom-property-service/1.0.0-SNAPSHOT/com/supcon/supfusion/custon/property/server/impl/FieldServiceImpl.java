package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.Field;
import com.supcon.supfusion.custon.property.dao.mappers.FieldMapper;
import com.supcon.supfusion.custon.property.server.FieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class FieldServiceImpl implements FieldService {

    @Autowired
    private FieldMapper fieldDaoMapper;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Override
    public List<Field> getFields(String viewCode) {
        LambdaQueryWrapper<Field> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Field::getViewCode, viewCode);
        return fieldDaoMapper.selectList(wrapper);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Override
    public List<Field> getFieldsByDataGridCode(String dataGridCode) {
        LambdaQueryWrapper<Field> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Field::getDataGridCode, dataGridCode);
        return fieldDaoMapper.selectList(wrapper);
    }
}
