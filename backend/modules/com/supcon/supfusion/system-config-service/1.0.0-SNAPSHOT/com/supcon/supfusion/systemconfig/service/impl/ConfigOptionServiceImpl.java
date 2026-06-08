package com.supcon.supfusion.systemconfig.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.systemconfig.dao.ConfigOptionMapper;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigOptionPO;
import com.supcon.supfusion.systemconfig.service.ConfigOptionService;
import org.springframework.stereotype.Service;

/**
 * @author lifangyuan
 */
@Service
public class ConfigOptionServiceImpl extends ServiceImpl<ConfigOptionMapper, ConfigOptionPO> implements ConfigOptionService {
}
