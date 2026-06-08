/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.annotation.RecipientRule;
import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年9月22日 下午3:24:13
 */
@RecipientRule(RecipientSelection.ROLE)
@Service("roleRuleService")
public class RoleRuleService extends AbstractRuleService {

    @Autowired
    @Lazy
    private UserServiceAdapter userServiceAdapter;
    /**
     * @see com.supcon.supfusion.flow.engine.server.service.RuleService#parse(java.lang.Object, java.lang.String)
     */
    @Override
    public Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId) {
        if (StringUtils.isEmpty(rule.getValue())) {
            return new ArrayList<>(1);
        }
        // 获取角色成员
        Collection<PersonDetailDTO> roleMembers = userServiceAdapter.queryRoleMember(rule.getValue());
        if (!roleMembers.isEmpty()) {
            // 过滤出能满足条件能收到待办的人员
            super.filter(roleMembers, rule);
        }
        return roleMembers;
    }
    
    /**
     * @see com.supcon.supfusion.flow.taskcenter.service.rule.RuleService#setTemporaryPersonMCache(java.util.Map)
     */
    @Override
    public void setTemporaryPersonCache(Map<Long, PersonDetailDTO> personMap) {
        // nothing to do
        
    }

}
