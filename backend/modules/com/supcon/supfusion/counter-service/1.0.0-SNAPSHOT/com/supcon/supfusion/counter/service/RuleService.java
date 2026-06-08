package com.supcon.supfusion.counter.service;


import com.supcon.supfusion.counter.service.bo.RuleBO;

public interface RuleService  {
    Long add(RuleBO rule);

    void modify(RuleBO rule);

    void delete(Long ruleId);

    RuleBO find(Long ruleId);
}
