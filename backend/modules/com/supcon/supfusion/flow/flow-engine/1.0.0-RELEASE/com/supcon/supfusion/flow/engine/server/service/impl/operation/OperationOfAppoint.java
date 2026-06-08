/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl.operation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.engine.RuntimeService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.annotation.OperationType;
import com.supcon.supfusion.flow.common.dto.AssigneeDTO;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.engine.server.service.OperationApi;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:40:54
 */
@OperationType(value = OperationTypeEnum.APPOINT)
@Component
public class OperationOfAppoint implements OperationApi {

    @Autowired
    @Lazy
    private RuntimeService runtimeService;
    
    /**
     * @see OperationApi#changeRecipient(org.flowable.task.service.delegate.DelegateTask)
     */
    @Override
    public Set<String> changeRecipient(DelegateTask delegateTask) {
        // 获取原始候选人, 候选人之间优先级相等
        Set<String> candidateUsers = delegateTask.getCandidates().stream()
                .filter(idLink -> Constants.CANDIDATE.equals(idLink.getType()))
                .map(IdentityLink :: getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (String candidateUser : candidateUsers) {
            delegateTask.deleteCandidateUser(candidateUser);
        }
        List<AssigneeDTO> assigns = LocalContext.getContext().getAssigns();
        for (AssigneeDTO assignment : assigns) {
            if (delegateTask.getTaskDefinitionKey().equals(assignment.getTaskDefKey())) {
                delegateTask.addCandidateUsers(assignment.getUsers());
                return assignment.getUsers();
            }
        }
        return new HashSet<>();
    }

}
