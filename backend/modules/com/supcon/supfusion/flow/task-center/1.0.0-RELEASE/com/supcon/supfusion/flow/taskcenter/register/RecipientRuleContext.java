/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.register;

import java.util.Collection;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.taskcenter.service.rule.RuleService;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:27:30
 */
@Slf4j
public class RecipientRuleContext {
    
    private Map<Object, Class<?>> instanceMap;
    private ApplicationContext springContext;
    
    public RecipientRuleContext(Map<Object, Class<?>> instanceMap, ApplicationContext springContext) {
        this.instanceMap = instanceMap;
        this.springContext = springContext;
    }
    
    public RuleService<RecipientRuleDTO, Collection<PersonDetailDTO>> getInstance(RecipientSelection recipientRule) {
        if (recipientRule == null) {
            return null;
        }
        Class<?> clazz = instanceMap.get(recipientRule);
        if (clazz == null) {
            log.error("当前操作类型不存在, 请求类型: {}", recipientRule);
            return null;
        }
        return (RuleService<RecipientRuleDTO, Collection<PersonDetailDTO>>)springContext.getBean(clazz);
    }
}
