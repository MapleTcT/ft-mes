/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service;

import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;

import com.supcon.supfusion.flow.common.dto.ProcessStartDTO;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月21日 下午6:41:49
 */
public interface ProcessEngineService {
    
    /**
     * 获取流程发起者ID
     * @param processId 流程ID
     * @return
     */
    String getInitiator(String processId);
    
    /**
     * 获取流程变量
     * @param processId
     * @param key
     * @param clazz
     * @return
     */
    <T> T getVariableValue(String processId, String key, Class<T> clazz);
    /**
     * 设置当前流程变量
     * @param processId
     * @param key
     * @param value
     */
    <T> void setVariable(String processId, String key, T value);
    
    /**
     * 获取启动节点信息
     * @param processKey 流程编码
     * @param processVersion 流程版本
     * @return 
     */
    ProcessStartDTO getProcessStartInfo(String processKey, int processVersion, String tenantId, boolean detail) throws DocumentException;
    
    /**
     * 启动流程
     * @param processKey 流程编号
     * @param processVersion 当前启用流程的版本号
     * @param processVariables 流程启动参数, 提供流程必须的参数
     * @param starter 流程启动者
     * @param tenantId 租户ID
     * @return 流程实例ID
     */
    String startUp(String processKey, int processVersion, String starter, String tenantId, Map<String, Object> variables, Map<String, Object> transientVariables);
    
    /**
     * 将一个流程模板下的运行实例迁移到另一个流程模板
     * @param oldProcessDefinitionId 老流程模板ID
     * @param newProcessDefinitionId 新流程模板ID
     * @param newProcessDefinitionVersion 新流程模板版本ID
     * @param tenantId 租户ID
     * @throws DocumentException 
     */
    void migrateProcessInstanceWithTenant(String oldProcessDefinitionId, int newProcessDefinitionVersion, String tenantId);
    
    /**
     * 
     * 批量删除流程实例
     * @param activeProcessIds 进行中的流程
     * @param completeProcessIds 已完成的流程
     */
    void batchDeleteProcessInstances(List<String> activeProcessIds, List<String> completeProcessIds);
    
    /**
     * 暂停流程
     * @param processId 流程实例ID
     */
    void suspendProcess(String processId);
    
    /**
     * 恢复流程
     * @param processId 流程实例ID
     */
    void activeProcess(String processId);
    
    /**
     * 终止流程
     * @param processId 流程实例ID
     */
    void terminateProcess(String processId);
    
    /**
     * 获取流程状态
     * @param processId
     * @return
     */
    ProcessStatusEnum getProcessStatus(String processId);
}
