package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.dao.entity.DataGrid;
import com.supcon.supfusion.custon.property.dao.mappers.DataGridMapper;
import com.supcon.supfusion.custon.property.server.DataGridService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class DataGridServiceImpl extends ServiceImpl<DataGridMapper, DataGrid> implements DataGridService {
}
