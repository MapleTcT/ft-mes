/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.taskcenter.service.rule.PersonRuleService;

/**
 * @author: zhuangmh
 * @date: 2021年2月24日 下午3:09:00
 */
@Component
public class FlowSchedule {

    @Autowired
    private PersonRuleService personRuleService;
    
    @Scheduled(fixedRate = 1800 * 1000)
    public void clearPersonCache() {
        personRuleService.clearTemporaryPersonCache();
    }
}
