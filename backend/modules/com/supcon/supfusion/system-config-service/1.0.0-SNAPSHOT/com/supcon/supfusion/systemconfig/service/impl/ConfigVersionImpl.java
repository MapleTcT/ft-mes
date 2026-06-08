package com.supcon.supfusion.systemconfig.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.systemconfig.dao.ConfigVersionMapper;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigVersionPO;
import com.supcon.supfusion.systemconfig.service.ConfigVersionService;
import org.springframework.stereotype.Service;

@Service
public class ConfigVersionImpl extends ServiceImpl<ConfigVersionMapper, ConfigVersionPO> implements ConfigVersionService {
}
