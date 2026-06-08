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

import lombok.extern.slf4j.Slf4j;

/**
 * 隔级领导规则解析
 * 
 * @author: zhuangmh
 * @date: 2020年9月22日 下午3:24:13
 */
@Slf4j
@RecipientRule(RecipientSelection.INITIATOR_SUPERIOR_LEADER)
@Service("initiatorSuperiorLeaderRuleService")
public class InitiatorSuperiorLeaderRuleService extends AbstractRuleService {
    
    @Autowired
    @Lazy
    private ProcessEngineService processEngineService;
    @Autowired
    @Lazy
    private UserServiceAdapter userServiceAdapter;
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.RuleService#parse(java.lang.Object, java.lang.String)
     */
    @Override
    public Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId) {
        String userId = processEngineService.getInitiator(processId);
        if (StringUtils.isEmpty(userId)) {
            log.error("无法从上下文环境获取流程发起者, 流程ID={}", processId);
            return new HashSet<>();
        }  
        UserOrgDetailDTO user = userServiceAdapter.getUserById(Long.parseLong(userId));
        if (user == null) {
            return new HashSet<>();
        }
        PersonDetailDTO superiorLeader = organizationServiceAdapter.getSuperiorLeader(user.getPersonId());
        List<PersonDetailDTO> leaders = new ArrayList<>();
        if (superiorLeader != null) {
            leaders.add(superiorLeader);
        }
        return leaders;
    }
    
    /**
     * @see com.supcon.supfusion.flow.taskcenter.service.rule.RuleService#setTemporaryPersonMCache(java.util.Map)
     */
    @Override
    public void setTemporaryPersonCache(Map<Long, PersonDetailDTO> personMap) {
        // nothing to do
    }

}
