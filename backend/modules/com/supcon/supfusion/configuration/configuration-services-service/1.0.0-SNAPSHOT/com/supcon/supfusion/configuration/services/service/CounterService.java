package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.CounterRule;
import com.supcon.supfusion.configuration.services.entity.CounterRuleInfo;

/**
 * @Author kk.C
 * @Description 编码生成器service
 * @Date 2020/10/27 10:13
 * @Param
 * @return
 **/
public interface CounterService {
    Long add(CounterRule rule);

    void delete(Long ruleId);

    void modify(CounterRule rule);

    CounterRule find(Long ruleId);

    CounterRule findByName(String ruleName);
}
