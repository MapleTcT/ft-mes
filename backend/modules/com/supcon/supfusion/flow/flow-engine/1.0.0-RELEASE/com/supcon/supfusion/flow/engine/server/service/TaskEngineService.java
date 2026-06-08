/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 上午9:41:28
 */
public interface TaskEngineService {
    
    /**
     * 待办提交
     * @param taskInstanceId 待办ID
     * @param processInstanceId 流程实例ID
     * @param userId 执行者
     * @param variables 待办提交所需的变量--持久化
     * @param transientVariables 待办提交所需的变量--不需要持久化
     */
    void complete(String taskInstanceId, String processInstanceId, String userId, Map<String, Object> variables, Map<String, Object> transientVariables);
    
    /**
     * 判断待办是否需要自动提交
     * @param taskInstanceId 待办ID
     * @return true - 需要自动提交  false - 不需要
     */
    boolean detectAutoComplete(String taskInstanceId);
    
    /**
     * 撤回
     * @param taskInstanceId 已完成的待办实例ID
     * @param processId 流程实例ID
     * @param currentUserTaskKeys 需要被撤回的任务节点
     * @param engineVariables 必要的引擎变量
     */
    void revoke(String taskInstanceId, String processId, Set<String> currentUserTaskKeys, Map<String, Object> engineVariables);
    
    /**
     * 加签 - 受邀人参与会签
     * @param taskInstanceId 待办实例ID
     * @param invitees 受邀人列表
     * @param variables 上下文变量
     */
    List<String> joinMultiTask(String taskInstanceId, List<String> invitees, Map<String, Object> variables);

    /**
     * 迁移待办
     * @param taskInstanceId 待办实例ID
     * @param processInstanceId 流程实例ID
     * @param targetTaskKey 目标待办key
     */
    String migrate(String taskInstanceId, String processInstanceId, String targetTaskKey);
    
}
