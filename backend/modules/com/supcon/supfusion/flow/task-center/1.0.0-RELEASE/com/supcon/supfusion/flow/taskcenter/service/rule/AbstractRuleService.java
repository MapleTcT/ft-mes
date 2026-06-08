/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.taskcenter.rpc.OrganizationServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年9月23日 下午7:05:45
 */
public abstract class AbstractRuleService implements RuleService<RecipientRuleDTO, Collection<PersonDetailDTO>> {
    /**
     * 临时缓存人员信息
     */
    protected Map<Long, PersonDetailDTO> temporaryPersonCache = new ConcurrentHashMap<>();
    
    @Autowired
    @Lazy
    protected OrganizationServiceAdapter organizationServiceAdapter;
    
    public abstract Collection<PersonDetailDTO> parse(RecipientRuleDTO rule, String processId);
    
    protected void filter(Collection<PersonDetailDTO> unlimitPersons, RecipientRuleDTO rule) {
        // 无限制则不需要过滤
        if (rule.isUnrestrict()) {
            return;
        }
        if (!rule.isPosRestrict() && rule.getPositions() == null && rule.getPersons() == null) {
            return;
        }
        Long staffId = UserContext.getUserContext().getStaffId();
        Long companyId = UserContext.getUserContext().getCompanyId();
        Iterator<PersonDetailDTO> iterator = unlimitPersons.iterator();
        while(iterator.hasNext()) {
            Long personId = iterator.next().getId();
            boolean bingo = false;
            // 开启岗位限制, 规则如下:
            // 假设当前操作用户所在岗位A, 下一个环节待办接收者所在岗位B, 如果B为A的上级岗位,则可以收到待办并处理,否则无法接收.
            if (rule.isPosRestrict()) {
                bingo = organizationServiceAdapter.validateSuperiorSubordinate(staffId, personId, companyId);
            }
            // 指定岗位限制
            if (rule.getPositions() != null && !bingo) {
                // 验证提交者所在岗位和指定岗位是否存在上下级关系
                bingo = validateSpecifyPosition(staffId, rule.getPositions());
            }
            // 指定人员限制
            if (rule.getPersons() != null && !bingo) {
                bingo = validateSpecifyPerson(staffId, rule.getPersons());
            }
            if (!bingo) {
                iterator.remove();
            }
        }
    }
    
    private boolean validateSpecifyPosition(Long staffId, List<String> specifyPositions) {
        // 查询当前提交者所在岗位(下级岗位)
        Collection<PositionDetailDTO> positionsA = organizationServiceAdapter.getPosition(staffId);
        for (PositionDetailDTO positionA : positionsA) {
            for (String specifyPosition : specifyPositions) {
                boolean result = organizationServiceAdapter.validatePositionSuperiorSubordinate(Long.parseLong(specifyPosition), positionA.getId());
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean validateSpecifyPerson(Long curPersonId, List<String> specifyPersons) {
        return specifyPersons.contains(curPersonId.toString());
    }
}
