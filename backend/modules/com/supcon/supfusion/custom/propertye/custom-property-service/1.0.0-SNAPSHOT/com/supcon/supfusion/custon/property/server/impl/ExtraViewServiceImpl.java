package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.dao.entity.ExtraView;
import com.supcon.supfusion.custon.property.dao.mappers.ExtraViewMapper;
import com.supcon.supfusion.custon.property.server.ExtraViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class ExtraViewServiceImpl extends ServiceImpl<ExtraViewMapper, ExtraView> implements ExtraViewService {
}
