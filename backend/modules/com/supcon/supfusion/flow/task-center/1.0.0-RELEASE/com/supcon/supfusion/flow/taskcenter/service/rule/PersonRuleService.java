/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.annotation.RecipientRule;
import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年9月22日 下午3:24:13
 */
@RecipientRule(RecipientSelection.PERSON)
@Service("personRuleService")
public class PersonRuleService extends AbstractRuleService {

    /**
     * @see com.supcon.supfusion.flow.engine.server.service.RuleService#parse(java.lang.Object, java.lang.String)
     */
    @Override
    public Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId) {
        if (StringUtils.isEmpty(rule.getValue())) {
            return new ArrayList<>(1);
        }
        Long id = Long.parseLong(rule.getValue());
        PersonDetailDTO person = temporaryPersonCache.get(id);
        if (person == null) {
            person = organizationServiceAdapter.getPerson(id);
        }
        List<PersonDetailDTO> persons = new ArrayList<>(1);
        if (person == null) {
            return persons;
        }
        persons.add(person);
        // 过滤出能满足条件能收到待办的人员
        super.filter(persons, rule);
        return persons;
    }

    /**
     * @see com.supcon.supfusion.flow.taskcenter.service.rule.RuleService#setTemporaryPersonMCache(java.util.Map)
     */
    @Override
    public void setTemporaryPersonCache(Map<Long, PersonDetailDTO> personMap) {
        super.temporaryPersonCache = personMap;
    }
    
    /**
     * 
     */
    public void clearTemporaryPersonCache() {
        super.temporaryPersonCache.clear();
    }

}
