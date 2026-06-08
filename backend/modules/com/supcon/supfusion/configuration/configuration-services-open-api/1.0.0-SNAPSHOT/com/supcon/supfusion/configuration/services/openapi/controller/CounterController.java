package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.configuration.services.entity.CounterRule;
import com.supcon.supfusion.configuration.services.entity.CounterRuleInfo;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.service.CounterService;
import com.supcon.supfusion.configuration.services.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class CounterController extends ConfigurationBaseController {

    @Autowired
    private CounterService counterService;

    @ResponseBody
    @RequestMapping(value = "/ec/counter/add")
    public Long add(@RequestBody CounterRule counterRule) {
        return counterService.add(counterRule);
    }

    @RequestMapping(value = "/ec/counter/delete")
    public void delete(@RequestParam("ruleId")Long ruleId) {
        counterService.delete(ruleId);
    }

    @RequestMapping(value = "/ec/counter/update")
    public void update(@RequestBody CounterRule counterRule) {
        counterService.modify(counterRule);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/counter/find")
    public CounterRuleInfo find(@RequestParam("ruleId")Long ruleId) {
        CounterRuleInfo counterRuleInfo = new CounterRuleInfo();
        BeanUtils.copyProperties(counterService.find(ruleId),counterRuleInfo);
        return counterRuleInfo;
    }

}
