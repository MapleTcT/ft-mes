package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import com.supcon.supfusion.custon.property.dao.entity.PropertyProject;
import com.supcon.supfusion.custon.property.dao.mappers.PropertyMapper;
import com.supcon.supfusion.custon.property.dao.mappers.PropertyProjectMapper;
import com.supcon.supfusion.custon.property.server.PropertyProjectService;
import com.supcon.supfusion.custon.property.server.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class PropertyProjectServiceImpl extends ServiceImpl<PropertyProjectMapper, PropertyProject> implements PropertyProjectService {
}
