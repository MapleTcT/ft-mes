/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.service.impl;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.dto.DeploymentDTO;
import com.supcon.supfusion.flow.common.dto.DeploymentDTO.Builder;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.DiagramOperateException;
import com.supcon.supfusion.flow.common.util.BpmnModelUtils;
import com.supcon.supfusion.flow.common.util.DomUtils;
import com.supcon.supfusion.flow.engine.server.service.DeployService;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月20日 下午1:15:46
 */
@Service
public class DeployServiceImpl implements DeployService {

    private static final String BPMN_SUBFIX = ".bpmn20.xml";
    @Autowired
    private RepositoryService repositoryService;
    
    /**
     * @see DeployService#deploy(java.lang.String, java.lang.String)
     */
    @Override
    public DeploymentDTO deploy(String diagramCode, int version, String bpmnXml) throws UnsupportedEncodingException, DocumentException {
        return deployByTenantId(diagramCode, version, bpmnXml, null);
    }
    
    /**
     * @see DeployService#deployByTenantId(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public DeploymentDTO deployByTenantId(String diagramCode, int diagramVersion, String bpmnXml, String tenantId) throws UnsupportedEncodingException, DocumentException {
        String newBpmnXml = BpmnModelUtils.validateBpmnModel(bpmnXml);
        DeploymentBuilder builder = repositoryService
                .createDeployment()
                .addString(diagramCode + BPMN_SUBFIX, newBpmnXml)
                .key(diagramVersion + ""); // 关联引擎的流程版本和业务版本
        if (tenantId != null) {
            builder.tenantId(tenantId);
        }
        Deployment deployment = null;
        try {
            deployment = builder.deploy();
        } catch (Exception e) {
            throw new DiagramOperateException(FlowErrorEnum.DIAGRAM_STRUCTURE_ERROR, e);
        }
        ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        Builder deployDtoBuilder = new DeploymentDTO.Builder()
                .setProcessDefinitionId(newProcessDefinition.getId())
                .setProcessDefinitionVersion(newProcessDefinition.getVersion());
        return deployDtoBuilder.build();
    }

    /**
     * @throws DocumentException 
     * @see DeployService#getLatestDeployment(java.lang.String, java.lang.String)
     */
    @Override
    public DeploymentDTO getLatestDeployment(String diagramCode, int diagramVersion, String tenantId) throws DocumentException {
        // 查询上一次部署信息
        DeploymentQuery latestDeploymentQuery = repositoryService.createDeploymentQuery()
                .deploymentKey(diagramVersion + "")
                .processDefinitionKey(diagramCode);
        if (tenantId != null) {
            latestDeploymentQuery.deploymentTenantId(tenantId);
        }
        List<Deployment> latestDeployment = latestDeploymentQuery.orderByDeploymenTime().desc().listPage(0, 1);
        if (latestDeployment.isEmpty()) {
            return null;
        }
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(latestDeployment.get(0).getId()).singleResult();
        InputStream inputStream = repositoryService.getProcessModel(processDefinition.getId());
        Document document = DomUtils.getDocument(inputStream);
        return new DeploymentDTO.Builder()
                .setProcessDefinitionId(processDefinition.getId())
                .setProcessDefinitionXml(document.asXML())
                .build();
    }

    /**
     * @see DeployService#deleteDeploymentWithTenant(java.lang.String, int, java.lang.String)
     */
    @Override
    public Set<String> deleteDeploymentWithTenant(String diagramCode, int diagramVersion, String tenantId) {
        DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery()
                .processDefinitionKey(diagramCode)
                .deploymentKey(diagramVersion + "");
        if (tenantId != null) {
            deploymentQuery.deploymentTenantId(tenantId);
        }
        List<Deployment> depoymentList = deploymentQuery.list();
        Set<String> processDefinitionIds = new HashSet<>();
        for (Deployment depoyment : depoymentList) {
            repositoryService.deleteDeployment(depoyment.getId());
            processDefinitionIds.add(depoyment.getId());
        }
        return processDefinitionIds;
    }


}
