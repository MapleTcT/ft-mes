package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.InitVersionInfoMapper;
import com.supcon.supfusion.rbac.dao.po.InitVersionInfoPO;
import com.supcon.supfusion.rbac.service.IInitVersionInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InitVersionInfoServiceImpl extends ServiceImpl<InitVersionInfoMapper, InitVersionInfoPO> implements IInitVersionInfoService {
}
