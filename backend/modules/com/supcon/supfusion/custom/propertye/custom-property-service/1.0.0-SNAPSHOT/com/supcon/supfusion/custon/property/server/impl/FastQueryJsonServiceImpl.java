package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.dao.entity.ExtraView;
import com.supcon.supfusion.custon.property.dao.entity.FastQueryJson;
import com.supcon.supfusion.custon.property.dao.mappers.ExtraViewMapper;
import com.supcon.supfusion.custon.property.dao.mappers.FastQueryJsonMapper;
import com.supcon.supfusion.custon.property.server.FastQueryJsonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author zhang yafei
 */
@Service
public class FastQueryJsonServiceImpl extends ServiceImpl<FastQueryJsonMapper,FastQueryJson> implements FastQueryJsonService {

    @Autowired
    private FastQueryJsonMapper fastQueryJsonMapper;
    @Override
    public List<FastQueryJson> getFastQueryJsonByViewCode(String viewCode) {

        return fastQueryJsonMapper.selectList(new LambdaQueryWrapper<FastQueryJson>()
                .eq(FastQueryJson::getViewCode,viewCode));
    }
}
