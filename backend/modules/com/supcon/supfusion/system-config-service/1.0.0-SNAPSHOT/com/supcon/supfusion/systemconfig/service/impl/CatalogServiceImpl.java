package com.supcon.supfusion.systemconfig.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.systemconfig.dao.ConfigCatalogMapper;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigCatalogPO;
import com.supcon.supfusion.systemconfig.service.CatalogService;
import org.springframework.stereotype.Service;

/**
 * @author lifangyuan
 */
@Service
public class CatalogServiceImpl extends ServiceImpl<ConfigCatalogMapper, ConfigCatalogPO> implements CatalogService {
}
