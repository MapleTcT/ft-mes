package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.dao.entity.Validate;
import com.supcon.supfusion.custon.property.dao.mappers.ValidateMapper;
import com.supcon.supfusion.custon.property.server.ValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhang yafei
 */
@Service
public class ValidateServiceImpl extends ServiceImpl<ValidateMapper, Validate> implements ValidateService {
    @Autowired
    private ValidateMapper validateMapper;
    @Override
    public Set<Validate> getByFieldCode(String fieldCode) {
        List<Validate> validates = validateMapper.selectList(new LambdaQueryWrapper<Validate>().eq(Validate::getFieldCode, fieldCode));
        return new HashSet<>(validates);
    }
}
