/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.annotation.RecipientRule;
import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年9月22日 下午3:24:13
 */
@RecipientRule(RecipientSelection.VARIABLE)
@Service("variableRuleService")
public class VariableRuleService extends AbstractRuleService {

    @Autowired
    @Lazy
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    @Lazy
    private ProcessEngineService processEngineService;
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.RuleService#parse(java.lang.Object, java.lang.String)
     */
    @Override
    public Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId) {
        List<PersonDetailDTO> persons = new ArrayList<>();
        Object value = processEngineService.getVariableValue(processId, rule.getValue(), Object.class);
        if (value instanceof String && StringUtils.isNotEmpty(value.toString())) {
            String[] personIdArray = value.toString().split(Constants.COMMA);
            for (String id : personIdArray) {
                UserOrgDetailDTO user = userServiceAdapter.getUserById(Long.parseLong(id));
                if (user != null) {
                    PersonDetailDTO person = new PersonDetailDTO();
                    person.setId(user.getPersonId());
                    person.setUserId(user.getId());
                    persons.add(person);
                }
            }
        } else if (value instanceof List) {
            List<String> personList = (List)value;
            for (String id : personList) {
                UserOrgDetailDTO user = userServiceAdapter.getUserById(Long.parseLong(id));
                if (user != null) {
                    PersonDetailDTO person = new PersonDetailDTO();
                    person.setId(user.getPersonId());
                    person.setUserId(user.getId());
                    persons.add(person);
                }
            }
        }
        return persons;
    }
    
    /**
     * @see com.supcon.supfusion.flow.taskcenter.service.rule.RuleService#setTemporaryPersonMCache(java.util.Map)
     */
    @Override
    public void setTemporaryPersonCache(Map<Long, PersonDetailDTO> personMap) {
        // nothing to do
        
    }

}
