package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.MenuTempMapper;
import com.supcon.supfusion.rbac.dao.po.MenuTempPO;
import com.supcon.supfusion.rbac.service.IMenuTempService;
import org.springframework.stereotype.Service;

@Service
public class MenuTempServiceImpl extends ServiceImpl<MenuTempMapper, MenuTempPO> implements IMenuTempService {
}
