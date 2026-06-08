package com.supcon.supfusion.systemconfig.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.systemconfig.dao.ConfigInfoMapper;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigInfoPO;
import com.supcon.supfusion.systemconfig.service.ConfigInfoService;
import org.springframework.stereotype.Service;

/**
 * @author lifangyuan
 */
@Service
public class ConfigInfoServiceImpl extends ServiceImpl<ConfigInfoMapper, ConfigInfoPO> implements ConfigInfoService {
}
