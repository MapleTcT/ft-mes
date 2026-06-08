/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
@RecipientRule(RecipientSelection.INITIATOR)
@Service("initiatorRuleService")
public class InitiatorRuleService extends AbstractRuleService {

    @Autowired
    @Lazy
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    @Lazy
    private ProcessEngineService processEngineService;
    /**
     * 发起者
     * @see com.supcon.supfusion.flow.engine.server.service.RuleService#parse(java.lang.Object, java.lang.String)
     */
    @Override
    public Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId) {
        List<PersonDetailDTO> persons = new ArrayList<>();
        String initiator = processEngineService.getInitiator(processId);
        if (StringUtils.isNotEmpty(initiator)) {
            UserOrgDetailDTO user = userServiceAdapter.getUserById(Long.parseLong(initiator));
            if (user == null) {
                return new HashSet<>();
            }
            PersonDetailDTO person = new PersonDetailDTO();
            person.setUserId(user.getId());
            person.setId(user.getPersonId());
            person.setUserName(user.getUserName());
            persons.add(person);
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
