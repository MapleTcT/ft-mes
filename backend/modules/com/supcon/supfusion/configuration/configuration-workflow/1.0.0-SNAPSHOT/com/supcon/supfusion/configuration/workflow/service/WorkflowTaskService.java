package com.supcon.supfusion.configuration.workflow.service;

import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.Pending;
import com.supcon.supfusion.base.entities.Task;
import com.supcon.supfusion.configuration.workflow.entities.ExpectedConsign;
import org.jbpm.api.Execution;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.pvm.internal.model.ExecutionImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/25
 */
public interface WorkflowTaskService {

    Task getTask(String code, Long deployment);

    List<ExpectedConsign> getExpectedConsignList(String flowKey, String activeCode, int type);

    void expectedConsignRecallSchedule(List<ExpectedConsign> list,int type);

    void save(Task task);

    void dealMobileApprovePending(String processKey, int processVersion, Boolean flowMobileApprove, boolean isMobileApproveChanged);

    void setRecallAbleFlag(Boolean bool);

    void deleteFlowCurrentStatus(Execution execution);

    void invalid(OpenExecution execution);

    void effect(OpenExecution execution);

    Deployment getDeployment(Long deploymentId);

    boolean getRecallAbleFlag();

    String getLastTaskName();

    void saveFlowCurrentStatus(Execution execution);

    boolean checkDeploymentMobileApprove(Deployment deployment);

    void createPendings(List<Pending> pendings);

    Map<Long, List<Long>> getAssigeUser(OpenExecution execution,Deployment deployment);

    void getUserList(Map<String, Set<Long>> userIds, OpenExecution execution, String inputorFlag, String leaderFlag, String bigLeaderFlag, String staffIds, String flowDealFlag, String activityDealFlag, String attentFlag, Map<Long, String> entrust);

    void deletePendings(Long tableInfoId, String activityName);

    void deletePendings(String executionId);

    String getCountersignAssignStaff(Long deploymentId, String outcome, Long tableInfoId);

    void deleteCountersignAssignStaff(Long deploymentId, String outcome, Long tableInfoId);

    void saveCountersignAssignStaff(Long deploymentId, String outcome, String toString, Long tableInfoId);
}
