/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.annotation.RecipientRule;
import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.util.LocalContext;
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
@RecipientRule(RecipientSelection.SUBMITTER_DIRECT_LEADER)
@Service("submitterDirectLeaderRuleService")
public class SubmitterDirectLeaderRuleService extends AbstractRuleService {
    
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
        Long submitter = LocalContext.getContext().getSubmitter();
        if (submitter == null) {
            log.error("无法从上下文环境获取当前提交者, 流程ID={}", processId);
            return new ArrayList<>(1); 
        }
        // 根据用户ID获取人员ID
        UserOrgDetailDTO user = userServiceAdapter.getUserById(submitter);
        List<PersonDetailDTO> leaders = new ArrayList<>(1);
        if (user != null) {
            PersonDetailDTO directLeader = organizationServiceAdapter.getDirectLeader(user.getPersonId());
            if (directLeader != null) {
                leaders.add(directLeader);
            }
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
