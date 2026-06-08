package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.auth.dao.mapper.PwdRulesMapper;
import com.supcon.supfusion.auth.dao.po.PwdRulesPO;
import com.supcon.supfusion.auth.service.PwdRulesService;
import org.springframework.stereotype.Service;

@Service
public class PwdRulesServiceImpl extends ServiceImpl<PwdRulesMapper, PwdRulesPO> implements PwdRulesService {
}
