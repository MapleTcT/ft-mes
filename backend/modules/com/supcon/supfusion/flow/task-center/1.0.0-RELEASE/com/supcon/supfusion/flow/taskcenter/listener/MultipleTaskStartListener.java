/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.listener;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.dto.RecipientRuleDTO;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.engine.server.listener.AbstractMultipleTaskStartListener;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.register.RecipientRuleContext;
import com.supcon.supfusion.flow.taskcenter.service.rule.RuleService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年11月9日 下午1:51:07
 */
@Component
public class MultipleTaskStartListener extends AbstractMultipleTaskStartListener {

    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private RecipientRuleContext recipientRuleContext;
    @Autowired
    private ProcessEngineService processEngineService;
    @ServiceApiReference
    private PersonApiService orgService;
    /**
     * @see com.supcon.supfusion.flow.engine.server.listener.AbstractMultipleTaskStartListener#getMultipleTaskAssigneeValue(java.lang.String, java.lang.String)
     */
    @Override
    public Set<String> getMultipleTaskAssigneeValue(String taskDefinitionKey, String processDefinitionId, String processId) {
        Set<String> assigneeSet = new HashSet<>();
        List<RecipientRuleDTO> recipientRules = bpmnService.queryRecipientRules(taskDefinitionKey, processDefinitionId);
        // 一次性请求所有的人员信息
        List<Long> personIds = recipientRules.stream()
                .filter(r -> r.getRecipientSelect() == RecipientSelection.PERSON)
                .map(RecipientRuleDTO::getValue)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        for (RecipientRuleDTO recipientRule : recipientRules) {
            RuleService<RecipientRuleDTO, Collection<PersonDetailDTO>> ruleService = recipientRuleContext.getInstance(recipientRule.getRecipientSelect());
            if (!personIds.isEmpty()) {
                Map<Long, PersonDetailDTO> personMap = orgService.queryPersonsByIds(personIds);
                if (personMap != null && !personMap.isEmpty()) {
                    ruleService.setTemporaryPersonCache(personMap);
                }
            }
            Collection<PersonDetailDTO> persons = ruleService.parse(recipientRule, processId);
            Set<String> userIds = persons.stream().map(p -> p.getUserId().toString()).filter(Objects::nonNull).collect(Collectors.toSet());
            assigneeSet.addAll(userIds);
        }
        // 加签进来的用户, 在驳回时需要给他们重新生成待办
        OperationTypeEnum operationType = LocalContext.getContext().getOperationType();
        if (OperationTypeEnum.REJECT == operationType) {
            List<String> newInvitees = processEngineService.getVariableValue(processId, taskDefinitionKey, List.class);
            if (newInvitees != null) {
                assigneeSet.addAll(newInvitees);
            }
        }
        return assigneeSet;
    }

}
