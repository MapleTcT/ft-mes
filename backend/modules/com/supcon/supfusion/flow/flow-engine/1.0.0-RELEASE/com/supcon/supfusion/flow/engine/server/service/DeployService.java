/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.dom4j.DocumentException;

import com.supcon.supfusion.flow.common.dto.DeploymentDTO;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 下午1:16:31
 */
public interface DeployService {
    
    /**
     * 流程部署
     * @param diagramCode 流程编号
     * @param version 流程版本
     * @param bpmnXml bpmn结构数据
     * @return DeploymentDTO
     */
    DeploymentDTO deploy(String diagramCode, int version, String bpmnXml) throws UnsupportedEncodingException, DocumentException;
    /**
     * 流程部署到租户空间
     * @param diagramCode 流程编号
     * @param version 流程版本
     * @param bpmnXml bpmn结构数据
     * @param tenantId 租户ID
     * @return DeploymentDTO
     */
    DeploymentDTO deployByTenantId(String diagramCode, int version, String bpmnXml, String tenantId) throws UnsupportedEncodingException, DocumentException;
    
    /**
     * 
     * @Desc: 获取当前流程的上一次部署信息
     * @param diagramCode 流程编号
     * @param tenantId 租户ID
     * @return DeploymentDTO
     */
    DeploymentDTO getLatestDeployment(String diagramCode, int diagramVersion, String tenantId) throws DocumentException;
    
    /**
     * @Desc: 删除当前版本下发布的数据
     * @param diagramCode 流程编号
     * @param diagramVersion 流程版本号
     * @param tenantId 租户ID
     * @return 流程模板ID
     */
    Set<String> deleteDeploymentWithTenant(String diagramCode, int diagramVersion, String tenantId);
}
