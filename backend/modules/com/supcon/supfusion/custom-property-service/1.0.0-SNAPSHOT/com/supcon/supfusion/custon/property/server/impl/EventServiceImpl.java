package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.Event;
import com.supcon.supfusion.custon.property.dao.mappers.EventMapper;
import com.supcon.supfusion.custon.property.server.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class EventServiceImpl implements EventService {

    @Autowired
    private EventMapper eventMapper;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Override
    public List<Event> getEventsByLayoutCode(String layoutCode) {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Event::getLayoutCode,layoutCode);

        return eventMapper.selectList(wrapper);
    }

    @Override
    public Set<Event> getByFieldCode(String fieldCode) {
        List<Event> events = eventMapper.selectList(new LambdaQueryWrapper<Event>().eq(Event::getFieldCode, fieldCode));
        return new HashSet<>(events);
    }

    @Override
    public Set<Event> getByButtonCode(String buttonCode) {
        List<Event> events = eventMapper.selectList(new LambdaQueryWrapper<Event>().eq(Event::getButtonCode, buttonCode));
        return new HashSet<>(events);
    }
}
