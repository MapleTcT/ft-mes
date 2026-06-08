/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.supcon.supfusion.flow.api.OodmGatewayApi;
import com.supcon.supfusion.flow.common.dto.OodmAuditParamDTO;
import com.supcon.supfusion.flow.common.dto.OodmFlowParamDTO;
import com.supcon.supfusion.flow.common.dto.OodmSettingDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.TaskRuntimeException;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.webapi.AuditVO;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年12月2日 下午2:14:33
 */
@Service
@Slf4j
public class OodmServiceAdapter {
    
    @Autowired
    private OodmGatewayApi oodmGatewayApi;
    
    /**
     * 执行对象服务脚本
     * @param task 待办任务
     * @param audit 选择分支
     * @param formData 表单数据
     * @param oodmSetting oodm配置
     */
    public JsonObject executeService(PendingTaskPO task, AuditVO audit, String formData, OodmSettingDTO oodmSetting) {
        Map<String, Object> params = new HashMap<>();
        String flowParams = buildFlowParams(task);
        String auditParams = buildAuditParams(audit);
        params.put(Constants.FORM_DATA, formData);
        params.put(Constants.FLOW_DATA, flowParams);
        params.put(Constants.AUDIT_DATA, auditParams);
        try {
            String response = "";
            if (StringUtils.isEmpty(oodmSetting.getInstanceName())) {
                response = oodmGatewayApi.executeTemplateService(
                        oodmSetting.getTemplateNamespace(), 
                        oodmSetting.getTemplateName(), 
                        oodmSetting.getServiceNamespace(), 
                        oodmSetting.getServiceName(), 
                        params);
            } else {
                response = oodmGatewayApi.executeInstanceService(
                        oodmSetting.getTemplateNamespace(), 
                        oodmSetting.getTemplateName(), 
                        oodmSetting.getInstanceName(), 
                        oodmSetting.getServiceNamespace(), 
                        oodmSetting.getServiceName(), 
                        params);
            }
            JsonObject rresponseJsonObject = new JsonParser().parse(response).getAsJsonObject();
            int statusCode = rresponseJsonObject.get(Constants.CODE).getAsNumber().intValue();
            if (statusCode != Constants.SUCCESS_CODE) {
                String message = rresponseJsonObject.get(Constants.MESSAGE).getAsString();
                throw new TaskRuntimeException(FlowErrorEnum.OODM_EXECUTE_FAIL, message);
            } 
            JsonObject dataResult = rresponseJsonObject.get(Constants.DATA).getAsJsonObject();
            JsonObject resultJsonObject = dataResult.get(Constants.RESULT).getAsJsonObject();
            JsonElement codeElement = resultJsonObject.get(Constants.CODE);
            if (codeElement == null) {
                throw new TaskRuntimeException(FlowErrorEnum.OODM_EXECUTE_FAIL, "返回结果缺少code字段");
            }
            String bizCode = resultJsonObject.get(Constants.CODE).getAsString();
            if (!(Constants.SUCCESS_CODE + "").equals(bizCode)) {
                String message = resultJsonObject.get(Constants.MESSAGE).getAsString();
                throw new TaskRuntimeException(FlowErrorEnum.OODM_EXECUTE_FAIL, message);
            }
            JsonElement dataElement = resultJsonObject.get(Constants.DATA);
            if (dataElement != null) {
                return dataElement.getAsJsonObject();
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("服务脚本执行异常, 模板名: {}, 对象名: {}, 服务名: {}", oodmSetting.getTemplateName(), oodmSetting.getInstanceName(), oodmSetting.getServiceName(), e);
            throw new TaskRuntimeException(FlowErrorEnum.OODM_EXECUTE_FAIL, e, "服务脚本执行异常");
        }
        return null;
    }
    
    private String buildFlowParams(PendingTaskPO task) {
        if (task == null) {
            return "";
        }
        OodmFlowParamDTO flowparam = new OodmFlowParamDTO();
        flowparam.setCreateTime(task.getCreateTime());
        flowparam.setTaskName(task.getTaskDescriptionZhCn());
        flowparam.setTaskId(task.getId().toString());
        flowparam.setStaffName(task.getPersonName());
        flowparam.setProcessId(task.getProcessId());
        flowparam.setProcessName(task.getProcessName());
        flowparam.setUserId(task.getUserId().toString());
        flowparam.setInitiatorId(task.getInitiatorId());
        flowparam.setInitiatorName(task.getStaffName());
        return new Gson().toJson(flowparam);
    }
    
    private String buildAuditParams(AuditVO audit) {
        if (audit == null) {
            return "";
        }
        OodmAuditParamDTO flowparam = new OodmAuditParamDTO();
        flowparam.setName(audit.getName());
        flowparam.setOrder(audit.getOrder());
        flowparam.setReject(audit.getType() == Constants.REJECT);
        return new Gson().toJson(flowparam);
    }
}
